// IS_APPLICABLE: false
// ERROR: 'operator' modifier is inapplicable on this function: last parameter should not have a default value or be a vararg

class C {
    operator fun set(s: String, vararg value: Int): Boolean = true
}

class D(val c: C) {
    fun foo() {
        this.c.<caret>set("x", 1, 2)
    }
}
