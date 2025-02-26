class A
class B
class C

context(A)
class TopLevel<T1> {
    context(B)
    fun <T2> <expr>C</expr>.foo(param: Int, param2: String) {

    }
}

// LANGUAGE: +ContextReceivers