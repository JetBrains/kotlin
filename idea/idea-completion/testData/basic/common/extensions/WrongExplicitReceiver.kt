// FIR_COMPARISON
class Foo {
    fun Int.bar() = this
}

fun main(args: Array<String>) {
    val v = Foo()
    v.<caret>
}

// ABSENT: bar