// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

@JvmInline
value class DPoint(val x: Double, val y: Double)

fun box(): String {
    var res = 0.0
    for (x in listOf(DPoint(1.0, 2.0), DPoint(3.0, 4.0))) {
        res += x.x + x.y
    }
    require(res == 10.0)
    return "OK"
}
