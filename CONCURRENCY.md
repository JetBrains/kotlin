## Concurrency in Kotlin/Native

  Kotlin/Native runtime doesn't encourage a classical thread-oriented concurrency
 model with mutually exclusive code blocks and conditional variables, as this model is
 known to be error-prone and unreliable. Instead, we suggest a collection of
 alternative approaches, allowing you to use hardware concurrency and implement blocking IO.
 Those approaches are as follows, and they will be elaborated on in further sections:
   * Workers with message passing
   * Object subgraph ownership transfer
   * Object subgraph freezing
   * Object subgraph detachment
   * Raw shared memory using C globals
   * Atomic primitives and references
   * Coroutines for blocking operations (not covered in this document)

### Workers

  Instead of threads Kotlin/Native runtime offers the concept of workers: concurrently executed
 control flow streams with an associated request queue. Workers are very similar to the actors
 in the Actor Model. A worker can exchange Kotlin objects with another worker, so that at any moment
 each mutable object is owned by a single worker, but ownership can be transferred.
 See section [Object transfer and freezing](#transfer).

  Once a worker is started with the `Worker.start` function call, it can be addressed with its own unique integer
 worker id. Other workers, or non-worker concurrency primitives, such as OS threads, can send a message
 to the worker with the `execute` call.
 
<div class="sample" markdown="1" theme="idea" data-highlight-only>
  
 ```kotlin
val future = execute(TransferMode.SAFE, { SomeDataForWorker() }) {
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
  result -> println("result is $result")
}
```

</div>

 The call to `execute` uses a function passed as its second parameter to produce an object subgraph
 (i.e. set of mutually referring objects) which is then passed as a whole to that worker, it is then no longer
 available to the thread that initiated the request. This property is checked if the first parameter
 is `TransferMode.SAFE` by graph traversal and is just assumed to be true, if it is `TransferMode.UNSAFE`.
 The last parameter to `execute` is a special Kotlin lambda, which is not allowed to capture any state,
 and is actually invoked in the target worker's context. Once processed, the result is transferred to whatever consumes
 it in the future, and it is attached to the object graph of that worker/thread.

  If an object is transferred in `UNSAFE` mode and is still accessible from multiple concurrent executors,
 program will likely crash unexpectedly, so consider that last resort in optimizing, not a general purpose
 mechanism.

  For a more complete example please refer to the [workers example](https://github.com/JetBrains/kotlin-native/tree/master/samples/workers)
 in the Kotlin/Native repository.

<a name="transfer"></a>
### Object transfer and freezing

   An important invariant that Kotlin/Native runtime maintains is that the object is either owned by a single
  thread/worker, or it is immutable (_shared XOR mutable_). This ensures that the same data has a single mutator,
  and so there is no need for locking to exist. To achieve such an invariant, we use the concept of not externally
  referred object subgraphs.
  This is a subgraph which has no external references from outside of the subgraph, which could be checked
  algorithmically with O(N) complexity (in ARC systems), where N is the number of elements in such a subgraph.
  Such subgraphs are usually produced as a result of a lambda expression, for example some builder, and may not
  contain objects, referred to externally.

   Freezing is a runtime operation making a given object subgraph immutable, by modifying the object header
  so that future mutation attempts throw an `InvalidMutabilityException`. It is deep, so
  if an object has a pointer to other objects - transitive closure of such objects will be frozen.
  Freezing is a one way transformation, frozen objects cannot be unfrozen. Frozen objects have a nice
  property that due to their immutability, they can be freely shared between multiple workers/threads
  without breaking the "mutable XOR shared" invariant.

   If an object is frozen it can be checked with an extension property `isFrozen`, and if it is, object sharing
 is allowed. Currently, Kotlin/Native runtime only freezes the enum objects after creation, although additional
 autofreezing of certain provably immutable objects could be implemented in the future.

<a name="detach"></a>
### Object subgraph detachment

   An object subgraph without external references can be disconnected using `DetachedObjectGraph<T>` to
  a `COpaquePointer` value, which could be stored in `void*` data, so the disconnected object subgraphs
  can be stored in a C data structure, and later attached back with `DetachedObjectGraph<T>.attach()` in an arbitrary thread
  or a worker. Combining it with [raw memory sharing](#shared) it allows side channel object transfer between
  concurrent threads, if the worker mechanisms are insufficient for a particular task. Note, that object detachment
  may require explicit leaving function holding object references and then performing cyclic garbage collection.
  For example, code like:
```kotlin
val graph = DetachedObjectGraph {
    val map = mutableMapOf<String, String>()
    for (entry in map.entries) {
        // ...
    }
    map
}
```
  will not work as expected and will throw runtime exception, as there are uncollected cycles in the detached graph, while:
```kotlin
val graph = DetachedObjectGraph {
    {
     val map = mutableMapOf<String, String>()
     for (entry in map.entries) {
         // ...
     }
     map
    }().also {
      kotlin.native.internal.GC.collect()
    }
 }
```
 will work properly, as holding references will be released, and then cyclic garbage affecting reference counter is
 collected.

<a name="shared"></a>
### Raw shared memory

  Considering the strong ties between Kotlin/Native and C via interoperability, in conjunction with the other mechanisms
 mentioned above it is possible to build popular data structures, like concurrent hashmap or shared cache with
 Kotlin/Native. It is possible to rely upon shared C data, and store in it references to detached object subgraphs.
 Consider the following .def file:
 
<div class="sample" markdown="1" theme="idea" mode="c">

```c
package = global

---
typedef struct {
  int version;
  void* kotlinObject;
} SharedData;

SharedData sharedData;
```

</div>

After running the cinterop tool it can share Kotlin data in a versionized global structure,
and interact with it from Kotlin transparently via autogenerated Kotlin like this:

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
class SharedData(rawPtr: NativePtr) : CStructVar(rawPtr) {
    var version: Int
    var kotlinObject: COpaquePointer?
}
```

</div>

So in combination with the top level variable declared above, it can allow looking at the same memory from different
threads and building traditional concurrent structures with platform-specific synchronization primitives.

<a name="top_level"></a>
### Global variables and singletons

  Frequently, global variables are a source of unintended concurrency issues, so _Kotlin/Native_ implements
the following mechanisms to prevent the unintended sharing of state via global objects:

   * global variables, unless specially marked, can be only accessed from the main thread (that is, the thread
   _Kotlin/Native_ runtime was first initialized), if other thread access such a global, `IncorrectDereferenceException` is thrown
   * for global variables marked with the `@kotlin.native.ThreadLocal` annotation each threads keeps thread-local copy,
   so changes are not visible between threads
   * for global variables marked with the `@kotlin.native.SharedImmutable` annotation value is shared, but frozen
   before publishing, so each threads sees the same value
   * singleton objects unless marked with `@kotlin.native.ThreadLocal` are frozen and shared, lazy values allowed,
   unless cyclic frozen structures were attempted to be created
   * enums are always frozen

 Combined, these mechanisms allow natural race-free programming with code reuse across platforms in MPP projects.

<a name="atomic_references"></a>
### Atomic primitives and references

 Kotlin/Native standard library provides primitives for safe working with concurrently mutable data, namely
`AtomicInt`, `AtomicLong`, `AtomicNativePtr`, `AtomicReference` and `FreezableAtomicReference` in the package
`kotlin.native.concurrent`.
Atomic primitives allows concurrency-safe update operations, such as increment, decrement and compare-and-swap,
along with value setters and getters. Atomic primitives are considered always frozen by the runtime, and
while their fields can be updated with the regular `field.value += 1`, it is not concurrency safe.
Value must be be changed using dedicated operations, so it is possible to perform concurrent-safe
global counters and similar data structures.

  Some algorithms require shared mutable references across the multiple workers, for example global mutable
configuration could be implemented as an immutable instance of properties list atomically replaced with the
new version on configuration update as the whole in a single transaction. This way no inconsistent configuration
could be seen, and at the same time configuration could be updated as needed.
To achieve such functionality Kotlin/Native runtime provides two related classes:
`kotlin.native.concurrent.AtomicReference` and `kotlin.native.concurrent.FreezableAtomicReference`.
Atomic reference holds reference to a frozen or immutable object, and its value could be updated by set
or compare-and-swap operation. Thus, dedicated set of objects could be used to create mutable shared object graphs
(of immutable objects). Cycles in the shared memory could be created using atomic references.
Kotlin/Native runtime doesn't support garbage collecting cyclic data when reference cycle goes through
`AtomicReference` or frozen `FreezableAtomicReference`. So to avoid memory leaks atomic references
that are potentially parts of shared cyclic data should be zeroed out once no longer needed.

 If atomic reference value is attempted to be set to non-frozen value runtime exception is thrown.

 Freezable atomic reference is similar to the regular atomic reference, but until frozen behaves like regular box
for a reference. After freezing it behaves like an atomic reference, and can only hold a reference to a frozen object.
