// "Replace '@JvmField' with 'const'" "false"
// WITH_RUNTIME
// ERROR: JvmField has no effect on a private property
// ACTION: Make internal
// ACTION: Make protected
// ACTION: Make public
// ACTION: Move to constructor
// ACTION: Specify type explicitly
class Foo {
    <caret>@JvmField private val a = "Lorem ipsum"
}
