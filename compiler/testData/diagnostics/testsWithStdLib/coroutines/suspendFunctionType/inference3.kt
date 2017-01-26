// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> withS(x: T, sfn: suspend T.() -> Unit) = x

val test1 = withS(100) {}

fun <TT> test2(x: TT) = withS(x) {}