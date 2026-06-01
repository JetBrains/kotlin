class MyClass

operator fun MyClass.compareTo(other: Any?): Boolean = 5

fun main() {
    val x = <expr>MyClass() >= MyClass()</expr>
}
