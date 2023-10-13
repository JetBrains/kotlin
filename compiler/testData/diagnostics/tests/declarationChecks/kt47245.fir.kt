// !COMPILER_VERSION 2.0
// SKIP_KT_DUMP

fun test() {
    for (i in 0..0) <!SINGLE_ANONYMOUS_FUNCTION_WITH_NAME_WARNING!>fun x() {}<!>
}