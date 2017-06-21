// WITH_RUNTIME

val x = listOf(1, 2, 3).map { "$it*$it" }.<caret>joinToString(prefix = "= ", separator = " + ")