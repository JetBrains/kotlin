// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class)

data class C (
    val a : Int = 1,
    @IntroducedAt("1") val b: String = "",
    @IntroducedAt("2") val c: Float = 3f,
)

@Suppress("NON_ASCENDING_VERSION_ANNOTATION")
data class D (
    val a : Int = 1,
    @IntroducedAt("2") val a1: String = "3",
    @IntroducedAt("1") val b: Float = 1.0f,
)

fun test1() : String {
    val c = C()
    val copy1 = C::class.java.getMethod("copy", Int::class.java)
    val copy2 = C::class.java.getMethod("copy", Int::class.java, String::class.java)

    val r1 = copy1.invoke(c, c.a) as C
    val r2 = copy2.invoke(c, c.a, c.b) as C
    val r3 = c.copy()

    return if ((r1 == r2) && (r1 == r3)) "O" else "Err1: $r1 $r2 $r3 "
}

fun test2() : String {
    val d = D()
    val copy1 = D::class.java.getMethod("copy", Int::class.java)
    val copy2 = D::class.java.getMethod("copy", Int::class.java, Float::class.java)

    val r1 = copy1.invoke(d, d.a) as D
    val r2 = copy2.invoke(d, d.a, d.b) as D
    val r3 = d.copy()

    return if ((r1 == r2) && (r1 == r3)) "K" else " Err2: $r1 $r2 $r3"
}

fun box() = test1() + test2()
