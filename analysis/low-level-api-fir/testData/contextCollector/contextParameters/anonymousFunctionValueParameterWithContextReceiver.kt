class A
class B
class C
class D

context(A)
fun <T1> C.foo(param: Int, param2: String) {
    context(B)
    fun <T2> D.(<expr>anonParam: Boolean</expr>, anonParam2: Long) {

    }
}

// LANGUAGE: +ContextReceivers