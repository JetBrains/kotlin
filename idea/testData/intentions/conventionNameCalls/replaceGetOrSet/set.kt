// INTENTION_TEXT: Replace 'set' call with indexing operator

class C {
    operator fun set(s: String, value: Int) {}
}

fun foo() {
    C().<caret>set("x", 1)
}
