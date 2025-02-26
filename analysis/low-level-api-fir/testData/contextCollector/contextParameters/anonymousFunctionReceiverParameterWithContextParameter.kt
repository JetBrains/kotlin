class A
class B
class C
class D

context(a: A)
fun <T1> C.foo(param: Int, param2: String) {
    context(b: B)
    fun <T2> <expr>D</expr>.(anonParam: Boolean, anonParam2: Long) {

    }
}

// LANGUAGE: +ContextParameters