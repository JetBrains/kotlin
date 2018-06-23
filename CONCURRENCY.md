### Concurrency in Kotlin/Native

  Kotlin/Native runtime doesn't encourage a classical thread-oriented concurrency
 model with mutually exclusive code blocks and conditional variables, as this model is
 known to be error-prone and unreliable. Instead, we suggest collection of
 alternative approaches, allowing to use hardware concurrency and implement blocking IO.
 Those approaches are as following, and will be elaborated in further sections:
   * Workers with message passing
   * Object subgraph ownership transfer
   * Object subgraph freezing
   * Object subgraph detachment
   * Raw shared memory using C globals
   * Coroutines for blocking operations (not covered in this document)

 ## Workers

  Instead of threads Kotlin/Native runtime offers concept of workers: concurrently executing
 control flow streams with an associated request queue. Workers are very similar to actors
 in the Actor Model. Worker can exchange Kotlin objects with other workers, so that at the moment
 each mutable object is owned by the single worker, but ownership could be transferred.
 See section [Object transfer and freezing](#transfer).

  Once worker is started with `startWorker` function call, it can be uniquely addressed with an integer
 worker id. Other workers, or non-worker concurrency primitives, such as OS threads, could send a message
 to the worker with `schedule` call.
 ```kotlin
   val future = schedule(TransferMode.CHECKED, { SomeDataForWorker() }) {
       // data returned by the second function argument comes to the
       // worker routine as 'input' parameter.
       input ->
       // Here we create an instance to be returned when someone consumes result future.
       WorkerResult(input.stringParam + " result")
   }

   future.consume {
      // Here we see result returned from routine above. Note that future object or
      // id could be transferred to another worker, so we don't have to consume future
      // in same execution context it was obtained.
      result ->
      println("result is $result")
   }
```
 The call to `schedule` uses function passed as its second parameter to produce an object subgraph
 (i.e. set of mutually referring objects) which is passed as the whole to that worker, and no longer
 available to the thread that initiated the request. This property is checked if the first parameter
 is `TransferMode.CHECKED` by graph traversal and just assumed to be true, if it is `TransferMode.UNCHECKED`.
 Last parameter to schedule is a special Kotlin lambda, which is not allowed to capture any state,
 and is actually invoked in target worker's context. Once processed, result is transferred to whoever consumes
 the future, and is attached to object graph of that worker/thread.

  If an object is transferred in `UNCHECKED` mode and is still accessible from multiple concurrent executors,
 program will likely crash unexpectedly, so consider that last resort in optimizing, not a general purpose
 mechanism.

  For more complete example please refer to the [workers example](https://github.com/JetBrains/kotlin-native/tree/master/samples/workers)
 in the Kotlin/Native repository.


 ## <a name="transfer"></a>Object transfer and freezing

   Important invariant that Kotlin/Native runtime maintains is that object is either owned by a single
  thread/worker, or is immutable (_shared XOR mutable_). This ensures that the same data has a single mutator, and so no need for
  locking exists. To achieve such an invariant, we use concept of not externally referred object subgraphs.
  This is a subgraph which has no external references from outside of the subgraph, what could be checked
  algorithmically with O(N) complexity (in ARC systems), where N is number of elements in such a subgraph.
  Such subgraphs are usually produced as a result of lambda expression, for example some builder, and may not
  contain objects, referred externally.

   Freezing is a runtime operation making given object subgraph immutable, by modifying the object header
  so that future mutation attempts lead to throwing an `InvalidMutabilityException`. It is deep, so
  if an object has a pointer to another objects - transitive closure of such objects will be frozen.
  Freezing is the one way transformation, frozen objects cannot be unfrozen. Frozen objects have a nice
  property that due to their immutability, they can be freely shared between multiple workers/threads
  not breaking the "mutable XOR shared" invariant.

   If object is frozen could be checked with an extension property `isFrozen`, and if it is, object sharing
 is allowed. Currently, Kotlin/Native runtime only freezes enum objects after creation, although additional
 autofreezing of certain provably immutable objects could be implemented in the future.

  ## <a name="detach"></a>Object subgraph detachment

   Object subgraph without external references could be disconnected using `detachObjectGraph` to
  a `COpaquePointer` value, which could be stored in `void*` data, so disconnected object subgraphs
  could be stored in C data structure, and later attached back with `attachObjectGraph<T>` in arbitrary thread
  or worker. Combined with [raw memory sharing](#shared) it allows side channel object transfer between
  concurrent threads, if worker mechanisms are insufficient for the particular task.

 ## <a name="shared"></a>Raw shared memory

  Considering strong ties of Kotlin/Native with C via interoperability, in conjunction with other mechanisms
 mentioned above one could build popular data structures, like concurrent hashmap or shared cache with
 Kotlin/Native. One could rely upon shared C data, and store in it references to detached object subgraphs.
 Consider the following .def file:
```
package = global

---
typedef struct {
  int version;
  void* kotlinObject;
} SharedData;

SharedData sharedData;
```
After running cinterop tool it allows sharing Kotlin data in versionized global structure,
and interact with it from Kotlin transparently via autogenerated Kotlin like this:
```kotlin
class SharedData(rawPtr: NativePtr) : CStructVar(rawPtr) {
    var version: Int
    var kotlinObject: COpaquePointer?
}
```
So combined with the top level variable declared above, it allows seeing the same memory from different
threads and building traditional concurrent structures with platform-specific synchronization primitives.
