// WITH_RUNTIME
class MyClass(val function: (Int) -> Boolean)

fun foo() {
    MyClass(<caret>{ it == 7 })
}