class A
class B
class C

context(A)
class TopLevel<T1> {
    context(B)
    val <T2> <expr>C</expr>.foo get() = false
}

// LANGUAGE: +ContextReceivers