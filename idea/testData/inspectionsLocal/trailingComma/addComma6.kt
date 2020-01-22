// COMPILER_ARGUMENTS: -XXLanguage:+TrailingCommas
// FIX: Add trailing comma

val x = {
        x: String, y
    : String<caret>->
                val a = 2
}