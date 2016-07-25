// IS_APPLICABLE: false
// ERROR: None of the following functions can be called with the arguments supplied: <br>public constructor Foo() defined in Foo<br>public constructor Foo(x: String, y: String) defined in Foo

class Foo {
    <caret>lateinit var x: String

    constructor() {
        x = "Foo"
    }

    constructor(x: String, y: String): this(y.hashCode())
}