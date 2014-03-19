class Foo(val a: Int, b: Int) {
    val e: Int = <caret>
}

// EXIST: a
// EXIST: b