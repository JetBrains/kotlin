package foo

class CC<T>
class DD<T, T2>

val v1 = D<caret>D<C<caret>C>

// REF1: (foo).DD
// REF2: (foo).CC
