// IS_APPLICABLE: false

class C {
    operator fun set(s: String, vararg value: Int): Boolean = true
}

class D(val c: C) {
    fun foo() {
        this.c.<caret>set("x", 1, 2)
    }
}
