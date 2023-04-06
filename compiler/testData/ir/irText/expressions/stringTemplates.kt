// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

fun foo(): String = ""
val x = 42

val test1 = ""
val test2 = "abc"
val test3 = """"""
val test4 = """abc"""
val test5 = """
abc
"""
val test6 = "$test1 ${foo()}"

val test7 = "$test1"
val test8 = "${foo()}"
val test9 = "$x"
