// PROBLEM: none
// WITH_RUNTIME
class Foo {
    val prop = <caret>Obj.prop.toString()
}

object Obj {
    val prop = "Hello"
}