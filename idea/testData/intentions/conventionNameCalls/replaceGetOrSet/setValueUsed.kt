// IS_APPLICABLE: false

class C {
    operator fun set(s: String, value: Int): Boolean = true
}

class D(val c: C) {
    fun foo(): Boolean {
        return this.c.<caret>set("x", 1)
    }
}
