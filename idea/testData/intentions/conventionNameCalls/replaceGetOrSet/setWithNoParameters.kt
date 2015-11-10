// IS_APPLICABLE: false

class C {
    operator fun set(){}
}

class D(val c: C) {
    fun foo() {
        this.c.<caret>set()
    }
}
