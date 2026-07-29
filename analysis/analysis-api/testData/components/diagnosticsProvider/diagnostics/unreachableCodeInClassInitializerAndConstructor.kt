// WITH_STDLIB
// ISSUE: KT-63221

// Initializer blocks and constructors are a part of the class control flow graph, so control flow diagnostics reported inside them belong
// to the class structure element and not to the structure elements of the initializer block or the constructor.
class Foo {
    init {
        error("First")
        error("Second")
    }

    constructor() {
        error("First")
        error("Second")
    }
}
