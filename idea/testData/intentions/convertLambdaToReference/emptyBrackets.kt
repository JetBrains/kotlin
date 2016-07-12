// WITH_RUNTIME

fun bar(s: String) = s.length

val x = listOf("Jack", "Tom").map() <caret>{ w -> bar(w) }