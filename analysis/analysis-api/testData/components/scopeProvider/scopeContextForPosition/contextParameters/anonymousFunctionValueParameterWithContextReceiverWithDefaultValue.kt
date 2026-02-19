class A
class B
class C
class D

context(A, B, C, D)
fun usage() = false

context(A)
fun <T1> C.foo(param: Int, param2: String) {
    context(B)
    fun <T2> D.(anonParam: Boolean = <expr>usage()</expr>, anonParam2: Long) {

    }
}

// LANGUAGE: +ContextReceivers