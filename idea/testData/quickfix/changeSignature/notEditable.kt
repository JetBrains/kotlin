// "class org.jetbrains.kotlin.idea.quickfix.ChangeFunctionSignatureFix" "false"
//ERROR: No value passed for parameter other

abstract class StringComparable {
    public fun compareTo(other: String): Int = 0
}

class X: Comparable<String>, StringComparable()

fun main(args: Array<String>) {
    X().compareTo(<caret>)
}