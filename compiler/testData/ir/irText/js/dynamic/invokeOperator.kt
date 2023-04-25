// TARGET_BACKEND: JS_IR

// FIR_IDENTICAL
fun invoke() {}

fun test1(a: dynamic) = a(1)
fun test2(a: dynamic) = a.invoke(1)
fun test3(a: dynamic, b: dynamic) = a(b)
fun test4(a: dynamic, b: dynamic) = a.invoke(b)
fun test5(a: dynamic, b: dynamic) = a(b)(b)
fun test6(a: dynamic, b: dynamic) = a(b).invoke(b)
fun test7(a: dynamic) = invoke()
