// MODULE: original
class A(val x: Int) {
    init {
    }
}

// MODULE: copy
class A(val x: Int) {
    init {
        class B
        fun foo() {}
    }
}