// WITH_RUNTIME

val x = listOf(1, 0, 2).map { if (it != 0) it else null }.<caret>filterNotNull()