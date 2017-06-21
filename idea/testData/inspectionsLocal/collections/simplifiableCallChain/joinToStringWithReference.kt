// WITH_RUNTIME

val x = listOf(1, 2, 3).map(Int::toString).<caret>joinToString(prefix = "= ", separator = " + ")