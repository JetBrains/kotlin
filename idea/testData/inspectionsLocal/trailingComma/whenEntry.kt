// COMPILER_ARGUMENTS: -XXLanguage:+TrailingCommas
// FIX: Add line break

fun a() {
    when (val b = 5) {
        1, 2,
        3, <caret>->
    }
}