class A
class B
class C

context(A)
class TopLevel<T1> {
    context(B)
    fun <T2> C.foo(<expr>param: Int</expr>, param2: String) {

    }
}

// LANGUAGE: +ContextReceivers