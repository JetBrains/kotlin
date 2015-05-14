class C {
    companion object {
        fun plus(s: String): C = C()
    }
}

fun foo() {
    C.<caret>plus("x")
}
