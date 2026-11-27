// COMPILATION_ERRORS

class A {
    class B {
        class C<T>
    }
}

fun fo<caret>o(): A.B.C? = null
