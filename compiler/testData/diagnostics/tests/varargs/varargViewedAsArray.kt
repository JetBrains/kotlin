// DIAGNOSTICS: -UNUSED_PARAMETER

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
    useIntArray(<!TYPE_MISMATCH("(Array<Int>) -> Unit; KFunction1<IntArray, Unit>")!>::intVararg<!>)
    useMixedStringArgs1(<!TYPE_MISMATCH("(String, Array<String>) -> Unit; KFunction1<Array<out String>, Unit>")!>::stringVararg<!>)
    useMixedStringArgs2(<!TYPE_MISMATCH("(Array<String>, String) -> Unit; KFunction1<Array<out String>, Unit>")!>::stringVararg<!>)
    useMixedStringArgs3(<!TYPE_MISMATCH("(String, Array<String>, String) -> Unit; KFunction1<Array<out String>, Unit>")!>::stringVararg<!>)
    useTwoStringArrays(<!TYPE_MISMATCH("(Array<String>, Array<String>) -> Unit; KFunction1<Array<out String>, Unit>")!>::stringVararg<!>)
}
