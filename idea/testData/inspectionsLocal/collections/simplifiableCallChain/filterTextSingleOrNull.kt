// WITH_RUNTIME

val x = "5abc".filter { it.isDigit() }.<caret>singleOrNull()