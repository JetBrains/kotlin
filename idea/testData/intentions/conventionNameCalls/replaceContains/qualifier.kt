class C {
    companion object {
        fun contains(s: String) = true
    }
}

fun foo() {
    C.<caret>contains("x")
}
