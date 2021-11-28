class C(val x: Short)

fun box(): String {
    val a: Short = 1
    val b: C? = C(1)
    return if (a.equals(b?.x)) "OK" else "fail"
}
