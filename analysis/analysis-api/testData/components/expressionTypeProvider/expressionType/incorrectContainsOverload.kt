class MyClass

operator fun MyClass.contains(other: Any?): Int = 5

fun main() {
    val x = <expr>1 in MyClass()</expr>
}
