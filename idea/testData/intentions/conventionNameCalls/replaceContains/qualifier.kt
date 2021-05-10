class C {
    companion object {
        operator fun contains(s: String) = true
    }
}

fun foo() {
    C.<caret>contains("x")
}
