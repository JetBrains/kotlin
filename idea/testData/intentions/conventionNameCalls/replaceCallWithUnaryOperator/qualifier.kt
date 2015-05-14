class C {
    companion object {
        fun minus(): C = C()
    }
}

fun foo() {
    C.<caret>minus()
}
