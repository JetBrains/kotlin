// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun dyn(d: dynamic) {}

fun foo(d: dynamic): String = ""
fun foo(d: Int): Int = 1

fun nothing(d: dynamic): Int = 1
fun nothing(d: Nothing): String = ""

fun test(d: dynamic) {
    dyn(1)
    dyn("")

    foo(1).checkType { it : _<Int> }
    foo("").checkType { it : _<String> }

    // Checking specificity of `dynamic` vs `Nothing`
    nothing(d).checkType { it : _<String> }
    nothing("").checkType { it : _<Int> }
    [suppress("UNREACHABLE_CODE")] nothing(null!!).checkType { it : _<String> }
}