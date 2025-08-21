class A
class B
class C
class D

context(a: A)
fun <T1> C.foo(param: Int, param2: String) {
    context(b: B)
    fun <<expr>T2</expr>, T3> D.(anonParam: Boolean, anonParam2: Long) {

    }
}

// LANGUAGE: +ContextParameters