// COMPILER_ARGUMENTS: -XXLanguage:+TrailingCommas
// FIX: Add trailing comma
// DISABLE-ERRORS

fun a() {
    b(1, 3, 2424,
    awdawd<caret>)
}