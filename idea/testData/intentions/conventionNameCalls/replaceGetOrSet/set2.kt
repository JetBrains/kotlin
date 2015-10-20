// INTENTION_TEXT: Replace 'set' call with indexing operator

class C {
    operator fun set(s: String, value: Int) {}
}

class D(val c: C) {
    fun foo() {
        this.c.<caret>set("x", 1)
    }
}
