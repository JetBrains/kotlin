// PROBLEM: none
class MyClass(
    val function1: (Int) -> Boolean,
    val function2: (Int) -> Boolean
)

fun foo() {
    MyClass(<caret>{ it == 7 }, { it == 49 })
}