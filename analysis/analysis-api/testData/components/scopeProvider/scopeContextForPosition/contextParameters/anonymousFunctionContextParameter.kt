class A
class B
class C
class D

context(a: A)
fun <T1> C.foo(param: Int, param2: String) {
    context(<expr>b: B</expr>)
    fun <T2> D.(anonParam: Boolean, anonParam2: Long) {

    }
}

// LANGUAGE: +ContextParameters