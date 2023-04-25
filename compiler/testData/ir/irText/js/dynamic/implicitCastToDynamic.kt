// TARGET_BACKEND: JS_IR
val d1: dynamic = 1

val p: Int = 1

var d2: dynamic = p

fun withDynamic(d: dynamic) = d

fun test1(s: String) {
    withDynamic(s)
}

fun test2(a: Any) {
    val d: dynamic = a
}

fun test3(a: Any?) {
    val d: dynamic = a
}

fun test4(a: Any, s: String, na: Any?) {
    var d: dynamic = p
    d = a
    d = na
    d = s
}

fun test5(a: Any, s: String, na: Any?) {
    d2 = a
    d2 = na
    d2 = s
}
