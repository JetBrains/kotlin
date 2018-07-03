# Immutability in Kotlin/Native

 Kotlin/Native implements strict mutability checks, ensuring
important invariant that object is either immutable or
accessible from the single thread at the moment (`mutable XOR global`).

 Immutability is the runtime property in Kotlin/Native, and can be applied
to an arbitrary object subgraph using `konan.worker.freeze` function.
It makes all objects reachable from the given one immutable, and
such a transition is a one way operation (object cannot be unfrozen later).
Some naturally immutable objects, such as `kotlin.String`, `kotlin.Int` and
other primitive types, along with `AtomicInt` and `AtomicReference` are frozen
by default. If mutating operation is applied to a frozen object,
an `InvalidMutabilityException` is thrown.

 To achieve `mutable XOR global` invariant all globally visible state (currently,
`object` singletons and enums) are automatically frozen. If an object freezing
is not desirable, `konan.ThreadLocal` annotation could be used, which will make
object state thread local, and thus, mutable (but changed state not visible to
other threads).

 Class `AtomicReference` could be used to publish changed frozen state to
other threads, and thus build patterns like shared caches.

