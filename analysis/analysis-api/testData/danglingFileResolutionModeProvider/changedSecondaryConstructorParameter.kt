// MODULE: original
class A(val x: Int) {
    constructor(block: () -> Unit) : this(5) {}
}

// MODULE: copy
class A(val x: Int) {
    constructor(block: String) : this(5) {}
}