// WITH_RUNTIME

val x = listOf("1", "").filter <caret>{ element -> element.isNotEmpty() }.last()