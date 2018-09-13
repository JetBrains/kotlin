# Immutability in Kotlin/Native

 Kotlin/Native implements strict mutability checks, ensuring
the important invariant that the object is either immutable or
accessible from the single thread at that moment in time (`mutable XOR global`).

 Immutability is a runtime property in Kotlin/Native, and can be applied
to an arbitrary object subgraph using the `kotlin.native.concurrent.freeze` function.
It makes all the objects reachable from the given one immutable,
such a transition is a one-way operation (i.e., objects cannot be unfrozen later).
Some naturally immutable objects such as `kotlin.String`, `kotlin.Int`, and
other primitive types, along with `AtomicInt` and `AtomicReference` are frozen
by default. If a mutating operation is applied to a frozen object,
an `InvalidMutabilityException` is thrown.

 To achieve `mutable XOR global` invariant, all globally visible state (currently,
`object` singletons and enums) are automatically frozen. If object freezing
is not desired, a `kotlin.native.ThreadLocal` annotation can be used, which will make
the object state thread local, and so, mutable (but the changed state is not visible to
other threads).

 Top level/global variables of non-primitive types are by default accessible in the
main thread (i.e., the thread which initialized _Kotlin/Native_ runtime first) only.
Access from another thread will lead to an `IncorrectDereferenceException` being thrown.
To make such variables accessible in other threads, you can use either the `@ThreadLocal` annotation,
and mark the value thread local or `@SharedImmutable`, which will make the value frozen and accessible
from other threads.

 Class `AtomicReference` can be used to publish the changed frozen state to
other threads, and so build patterns like shared caches.

