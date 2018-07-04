// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

fun box(): String {
    val l = java.util.ArrayList<Int>()
    l.add(1000)

    val x = l[0] === 1000
    if (x) return "Fail: $x"
    val x1 = l[0] === 1
    if (x1) return "Fail 1: $x"
    val x2 = l[0] === l[0]
    if (!x2) return "Fail 2: $x"

    return "OK"
}
