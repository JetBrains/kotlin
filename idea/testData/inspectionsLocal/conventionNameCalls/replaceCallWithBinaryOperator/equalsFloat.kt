// PROBLEM: none

fun test(a: Any, b: Any) =
    a is Float && b is Float &&
            a.<caret>equals(b)