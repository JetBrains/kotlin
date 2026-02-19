// LANGUAGE: +SuspendConversion
// DIAGNOSTICS: -UNUSED_PARAMETER

fun unitCoercion(f: suspend () -> Unit) {}
fun foo(): Int = 0

fun defaults(f: suspend (Int) -> String) {}
fun bar(i: Int, l: Long = 42L): String = ""

fun varargs(f: suspend (Int, Int, Int) -> String) {}
fun baz(vararg ints: Int): String = ""

fun unitCoercionAndDefaults(f: suspend () -> Unit) {}
fun all(s: String = ""): Int = 0

fun box(): String {
    unitCoercion(::foo)
    defaults(::bar)
    varargs(::baz)
    unitCoercionAndDefaults(::all)
    return "OK"
}
