open class A(val a: String, val b: Int)

fun box(): String {
    val o = object : A(b = 2, a = "OK") {}
    return o.a
}
