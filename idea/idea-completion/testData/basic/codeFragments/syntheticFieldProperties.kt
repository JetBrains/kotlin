class Foo {
    val foo: Int = 3
        get() = field + 1

    fun foo() {
        <caret>
    }
}

// INVOCATION_COUNT: 1
// EXIST: foo
// EXIST: foo_field