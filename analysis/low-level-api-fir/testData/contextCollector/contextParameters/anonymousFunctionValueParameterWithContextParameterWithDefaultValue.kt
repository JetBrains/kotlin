class A
class B
class C
class D

context(_: A, _: B, _:C, _: D)
fun usage() = false

context(a: A)
fun <T1> C.foo(param: Int, param2: String) {
    context(b: B)
    fun <T2> D.(anonParam: Boolean = <expr>usage()</expr>, anonParam2: Long) {

    }
}

// LANGUAGE: +ContextParameters