class A
class B
class C

context(A)
class TopLevel<T1> {
    context(B)
    val <<expr>T2</expr>, T3> C.foo get() = false
}

// LANGUAGE: +ContextReceivers