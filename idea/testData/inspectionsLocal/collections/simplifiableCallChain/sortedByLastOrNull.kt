// WITH_RUNTIME
val x = listOf("a" to 1, "c" to 3, "b" to 2).<caret>sortedBy { it.second }.lastOrNull()