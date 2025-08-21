class A
class B
class C

context(A, B, C)
fun usage() = 0

context(A)
class TopLevel<T1> {
    context(B)
    fun <T2> C.foo(param: Int = <expr>usage()</expr>, param2: String) {

    }
}

// LANGUAGE: +ContextReceivers