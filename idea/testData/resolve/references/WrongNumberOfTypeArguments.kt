package foo

class A

class C<T>
class C1<T>

val v1 = C<C1<A, <caret>A>>()

// REF: (foo).A
