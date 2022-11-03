// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
// !LANGUAGE: +InlineClasses -AllowResultInReturnType

fun result(): Result<Int> = TODO()
val resultP: Result<Int> = result()
