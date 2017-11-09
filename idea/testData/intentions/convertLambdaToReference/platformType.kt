// WITH_RUNTIME

val y = java.lang.String.valueOf(42)
val x = y.let { <caret>it.capitalize() }