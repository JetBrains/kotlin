// FIR_IDENTICAL
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

    foo(1).checkType { _<Int>() }
    foo("").checkType { _<String>() }

    // Checking specificity of `dynamic` vs `Nothing`
    nothing(d).checkType { _<String>() }
    nothing("").checkType { _<Int>() }
    @Suppress("UNREACHABLE_CODE") nothing(null!!).checkType { _<String>() }
}