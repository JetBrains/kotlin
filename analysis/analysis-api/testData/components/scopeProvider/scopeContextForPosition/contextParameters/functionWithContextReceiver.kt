class A
class B
class C

context(A)
class TopLevel<T1> {
    <expr>context(B)
    fun <T2> C.foo(param: Int, param2: String) {

    }</expr>
}

// LANGUAGE: +ContextReceivers