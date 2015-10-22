// INTENTION_TEXT: Replace 'set' call with indexing operator

class C {
    operator fun set(s: String, p: Int, value: Int): Boolean = true
}

class D(val c: C) {
    fun foo() {
        this.c.<caret>set("x", 2, 1)
    }
}
