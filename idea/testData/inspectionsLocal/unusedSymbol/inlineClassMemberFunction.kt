// PROBLEM: none

inline class InlineClass(val x: Int) {
    fun <caret>takeInline() = 1
}

val call = InlineClass(1).takeInline()