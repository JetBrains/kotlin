abstract class Base {
    abstract fun foo(): Int
}

class Derived : Base() {
    override fun foo() = <caret>
}

fun getInt(): Int = 0
fun getString(): String = ""

// EXIST: getInt
// ABSENT: getString