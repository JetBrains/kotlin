// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

val p: Any? = null

fun foo(): Any? = null

fun test1(a: Any?, b: Any) = a ?: b

fun test2(a: String?, b: Any) = a ?: b

fun test3(a: Any?, b: Any?): String {
    if (b !is String) return ""
    if (a !is String?) return ""
    return a ?: b
}

fun test4(x: Any) = p ?: x

fun test5(x: Any) = foo() ?: x
