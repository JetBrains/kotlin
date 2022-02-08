class C(val x: Short)

fun box(): String {
    val a: C = C(1)
    val b: C? = C(1)
    return if (b?.x == a.x) "OK" else "fail"
}