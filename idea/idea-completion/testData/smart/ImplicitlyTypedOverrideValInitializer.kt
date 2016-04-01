abstract class Base {
    abstract val foo: Int
}

class Derived : Base() {
    override val foo = <caret>
}

fun getInt(): Int = 0
fun getString(): String = ""

// EXIST: getInt
// ABSENT: getString