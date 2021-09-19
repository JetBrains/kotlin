// !LANGUAGE: +ContextReceivers
// FIR_IDENTICAL

fun <T> f(t: @ContextFunctionTypeParams(42) T, tt: @ContextFunctionTypeParams(1) Int) {}

fun test() {
    val f: @ContextFunctionTypeParams(1) @ExtensionFunctionType (Int, String) -> Unit = {}
}