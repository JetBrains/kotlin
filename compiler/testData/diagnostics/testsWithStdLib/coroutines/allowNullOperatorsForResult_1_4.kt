// FIR_IDENTICAL
// LANGUAGE: +AllowNullOperatorsForResult
// DIAGNOSTICS: -UNUSED_EXPRESSION

fun test(r: Result<Int>?) {
    r ?: 0
    r?.isFailure
}