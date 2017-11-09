// PROBLEM: none
// WITH_RUNTIME

val x = listOf(1, 2, 3).map(Int::toDouble).<caret>joinToString(prefix = "= ", separator = " + ")