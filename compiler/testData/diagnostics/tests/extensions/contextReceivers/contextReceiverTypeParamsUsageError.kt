// FIR_IDENTICAL

fun <T> f(t: <!UNSUPPORTED_FEATURE!>@ContextFunctionTypeParams(42)<!> T, tt: <!UNSUPPORTED_FEATURE!>@ContextFunctionTypeParams(1)<!> Int) {}

fun test() {
    val f: <!UNSUPPORTED_FEATURE!>@ContextFunctionTypeParams(1)<!> @ExtensionFunctionType (Int, String) -> Unit = {}
}