// PROBLEM: none
// WITH_RUNTIME

val x = listOf("1", "").<caret>filter { it.isNotEmpty() }.first { it[0] == 'A' }