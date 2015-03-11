class A {
    default object {
        fun foo() = toString()
    }
}

val a = A.toString()
