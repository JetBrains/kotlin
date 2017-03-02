// JVM_TARGET: 1.8

fun test(o: Number) {}

fun test2(o: Number) {
    val p: Int = 1
    val o = if (z < 1) p else o
    test(o)
}

var z = 1

fun box(): String {
    val x: Number = 1
    test2(x)
    return "OK"
}