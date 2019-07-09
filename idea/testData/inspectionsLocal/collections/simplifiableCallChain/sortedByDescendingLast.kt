// WITH_RUNTIME
val x: Pair<String, Int> = listOf("a" to 1, "c" to 3, "b" to 2).<caret>sortedByDescending { it.second }.last()