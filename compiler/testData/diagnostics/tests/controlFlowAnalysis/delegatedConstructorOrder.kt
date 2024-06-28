// FIR_IDENTICAL
// DUMP_CFG
// ISSUE: KT-67456

class A {
    private val a: Any

    constructor(a: Any) {
        this.a = a
    }

    constructor(a: Any, b: Boolean) : this(a) {
        while (b) {}
    }
}

class B {
    private val a: Any

    constructor(a: Any, b: Boolean) : this(a) {
        while (b) { }
    }

    constructor(a: Any) {
        this.a = a
    }
}
