// WITH_STDLIB
class A(val k: Int) { fun f(x: Int) = x * k }
fun box(): String {
    var a = A(1)
    val s = sequenceOf(1, 2).map(a::f)
    a = A(10)
    val r = s.toList()
    return if (r == listOf(1, 2)) "OK" else "Fail: $r"
}
