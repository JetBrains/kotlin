// "class org.jetbrains.kotlin.idea.quickfix.RemoveFunctionParametersFix" "false"
//ERROR: No value passed for parameter other

trait StringComparable {
    public fun compareTo(other: String): Int = 0
}

class X: Comparable<String>, StringComparable

fun main(args: Array<String>) {
    X().compareTo(<caret>)
}