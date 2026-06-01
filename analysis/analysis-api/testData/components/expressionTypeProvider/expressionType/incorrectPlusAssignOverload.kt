class MyClass

operator fun MyClass.plusAssign(other: MyClass): Boolean {}

fun main() {
    <expr>MyClass() += MyClass()</expr>
}
