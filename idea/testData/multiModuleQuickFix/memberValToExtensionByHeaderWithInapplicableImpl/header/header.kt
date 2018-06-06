// "Convert member to extension" "false"
// ACTION: Convert property to function
// ACTION: Introduce backing property
// ACTION: Move to companion object
// ACTION: Move to constructor

expect class Foo {
    val <caret>foo: Int
}