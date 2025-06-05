// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class)

data class C (
    val a : Int = 1,
    @IntroducedAt("1") val b: String = "",
    @IntroducedAt("1") private val b1: String = "",
    @IntroducedAt("2") val c: Float = 3f,
)

@Suppress("NON_ASCENDING_VERSION_ANNOTATION")
data class D (
    val a : Int = 1,
    @IntroducedAt("2") val a1: String = "",
    @IntroducedAt("1") private val b: String = "",
    @IntroducedAt("1") val c: Float = 3f,
)

fun test1() : String {
    val c = C()

    val constructor1 = C::class.java.getConstructor(Int::class.java)
    val constructor2 = C::class.java.getConstructor(Int::class.java, String::class.java, String::class.java)

    val r1 = constructor1.newInstance(c.a) as C
    val r2 = constructor2.newInstance(c.a, c.b, "") as C

    return if ((r1 == c) && (r2 == c)) "O" else "Err1: $c $r1 $r2 "
}

fun test2() : String {
    val d = D()

    val constructor1 = D::class.java.getConstructor(Int::class.java)
    val constructor2 = D::class.java.getConstructor(Int::class.java, String::class.java, Float::class.java)

    val r1 = constructor1.newInstance(d.a) as D
    val r2 = constructor2.newInstance(d.a, "", d.c) as D

    return if ((r1 == d) && (r2 == d)) "K" else " Err2: $d $r1 $r2"
}

fun box() = test1() + test2()