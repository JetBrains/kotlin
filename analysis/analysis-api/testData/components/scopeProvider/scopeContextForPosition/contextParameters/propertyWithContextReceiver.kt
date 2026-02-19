class A
class B
class C

context(A)
class TopLevel<T1> {
    <expr>context(B)
    val <T2, T3> C.foo get() = false</expr>
}

// LANGUAGE: +ContextReceivers