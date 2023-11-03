// !DIAGNOSTICS: -UNUSED_PARAMETER

fun stringVararg(vararg args: String) {}
fun intVararg(vararg args: Int) {}
fun numberVararg(vararg args: Number) {}

fun useStrings(fn: (String, String, String) -> Unit) = fn("foo", "bar", "boo")
fun useStringArray(fn: (Array<String>) -> Unit) = fn(arrayOf("foo", "bar", "boo"))

fun useIntArray(fn: (Array<Int>) -> Unit) = fn(arrayOf(1, 2, 3))
fun usePrimitiveIntArray(fn: (IntArray) -> Unit) = fn(intArrayOf(4, 5, 6))

fun useMixedStringArgs1(fn: (String, Array<String>) -> Unit) = fn("foo", arrayOf("bar", "baz"))
fun useMixedStringArgs2(fn: (Array<String>, String) -> Unit) = fn(arrayOf("foo", "bar"), "baz")
fun useMixedStringArgs3(fn: (String, Array<String>, String) -> Unit) = fn("foo", arrayOf("bar", "baz"), "boo")
fun useTwoStringArrays(fn: (Array<String>, Array<String>) -> Unit) = fn(arrayOf("foo", "bar"), arrayOf("baz", "boo"))

fun test() {
    useStrings(::stringVararg)
    useStringArray(::stringVararg)
    useIntArray(::numberVararg)
    usePrimitiveIntArray(::intVararg)
    useIntArray(::<!INAPPLICABLE_CANDIDATE!>intVararg<!>)
    useMixedStringArgs1(::<!INAPPLICABLE_CANDIDATE!>stringVararg<!>)
    useMixedStringArgs2(::<!INAPPLICABLE_CANDIDATE!>stringVararg<!>)
    useMixedStringArgs3(::<!INAPPLICABLE_CANDIDATE!>stringVararg<!>)
    useTwoStringArrays(::<!INAPPLICABLE_CANDIDATE!>stringVararg<!>)
}
