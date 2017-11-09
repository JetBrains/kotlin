// PROBLEM: none
// WITH_RUNTIME

val x = listOf("1").<caret>mapNotNull { if (it.isNotEmpty()) it.toInt() else null }