class C {
    companion object {
        fun get(s: String): C = C()
    }
}

fun foo() {
    C.<caret>get("x")
}
