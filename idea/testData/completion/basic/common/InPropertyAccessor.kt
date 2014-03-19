class Foo(val a: Int, b: Int) {
    val e: Int
        get() = <caret>
}

// EXIST: a
// ABSENT: b