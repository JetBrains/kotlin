class A
class B
class C

context(A)
class TopLevel<T1> {
    context(B)
    fun <<expr>T2</expr>, T3> C.foo(param: Int, param2: String) {

    }
}

// LANGUAGE: +ContextReceivers