// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <vararg Ts> variadic(vararg args: *Ts) {}

val v1 = variadic(*Tuple<Any>())

val v2 = variadic(*<!MULTIPLE_VARIADIC_ARGUMENTS_WITH_SPREAD!>Tuple<Any>()<!>, *<!MULTIPLE_VARIADIC_ARGUMENTS_WITH_SPREAD!>Tuple<Any>()<!>)

val v3 = variadic(Unit, *<!MULTIPLE_VARIADIC_ARGUMENTS_WITH_SPREAD!>Tuple<Any>()<!>)

val v4 = variadic(42, Unit, *<!MULTIPLE_VARIADIC_ARGUMENTS_WITH_SPREAD!>Tuple<Any>()<!>)

val v5 = variadic(1, 2, 3, 4, Unit)
