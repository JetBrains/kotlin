// !LANGUAGE: +NewInference +VariadicGenerics
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Box<T>

fun <Ts> v0(args: Ts) {}
fun <Ts> v1(args: <!NON_VARIADIC_SPREAD!>*Ts<!>) {}
fun <Ts> v2(vararg args: Ts) {}
fun <Ts> v3(vararg args: <!NON_VARIADIC_SPREAD!>*Ts<!>) {}
fun <vararg Ts> v4(args: <!NO_SPREAD_FOR_VARIADIC_PARAMETER!>Ts<!>) {}
fun <vararg Ts> v5(args: <!NON_VARIADIC_SPREAD!>*Ts<!>) {}
fun <vararg Ts> v6(vararg args: <!NO_SPREAD_FOR_VARIADIC_PARAMETER!>Ts<!>) {}
fun <vararg Ts> v7(vararg args: *Ts) {}
fun <vararg Ts> v8(vararg args: <!UNSUPPORTED!>*Box<*Ts><!>) {}
fun <vararg Ts> v9(vararg args: <!NO_SPREAD_FOR_VARIADIC_PARAMETER!>Box<Ts><!>) {}
