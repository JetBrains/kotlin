// TARGET_BACKEND: JVM
// WITH_STDLIB

class V1<T : Number>(y: T) {
    @JvmField
    var x: T = y
}

class V2<T : Number> {
    lateinit var x: T
}

fun check(a: V1<Float>, b: V2<Float>): Boolean =
    a.x != b.x

fun box(): String {
    val v1 = V1(1.0f)
    val v2 = V2<Float>()
    v2.x = 2.0f
    return if (check(v1, v2)) "OK" else "Fail"
}
