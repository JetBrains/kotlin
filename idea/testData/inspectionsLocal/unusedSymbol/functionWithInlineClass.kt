// PROBLEM: none

inline class InlineClass(val x: Int)

fun <caret>takeInline(inlineClass: InlineClass) = 1

val call = takeInline(InlineClass(1))