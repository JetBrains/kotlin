// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE -NOTHING_TO_INLINE
// SKIP_TXT

suspend inline fun test1(<!NOT_YET_SUPPORTED_IN_INLINE!>s : suspend () -> String = { "OK" }<!>) {}
suspend inline fun test2(s : () -> String = { "OK" }) {}
suspend inline fun test3(<!NOT_YET_SUPPORTED_IN_INLINE!>crossinline s : suspend () -> String = { "OK" }<!>) {}
suspend inline fun test4(crossinline s : () -> String = { "OK" }) {}
suspend inline fun test5(noinline s : suspend () -> String = { "OK" }) {}
suspend inline fun test6(noinline s : () -> String = { "OK" }) {}

