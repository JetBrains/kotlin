// IS_APPLICABLE: false
// ERROR: There's a cycle in the delegation calls chain
// ERROR: There's a cycle in the delegation calls chain
// ERROR: There's a cycle in the delegation calls chain

class Foo {
    <caret>lateinit var bar: String

    constructor() {
        bar = ""
    }

    constructor(a: Int) : this(a, 0, 0) {
    }

    constructor(a: Int, b: Int) : this(a) {
    }

    constructor(a: Int, b: Int, c: Int) : this(a, b) {
    }
}
