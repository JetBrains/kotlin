// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

val p = 0
fun foo() = 1

fun test(a: IntArray) =
        a[0] + a[p] + a[foo()]
