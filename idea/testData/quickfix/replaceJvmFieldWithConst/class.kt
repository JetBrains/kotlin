// "Replace '@JvmField' with 'const'" "false"
// WITH_RUNTIME
// ERROR: JvmField has no effect on a private property
// ACTION: Move to constructor
// ACTION: Specify type explicitly
class Foo {
    <caret>@JvmField private val a = "Lorem ipsum"
}
