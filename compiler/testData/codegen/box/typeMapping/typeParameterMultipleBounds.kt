// TARGET_BACKEND: JVM

// WITH_RUNTIME

interface I1
interface I2
open class C

interface K {
    // Erasure of a type parameter with multiple bounds should be the class bound, or the first bound if there's no class bound

    fun <T> c1(t: T) where T : C, T : I1, T : I2
    fun <T> c2(t: T) where T : I1, T : C, T : I2
    fun <T> c3(t: T) where T : I2, T : C, T : I1
    fun <T> c4(t: T) where T : I2, T : I1, T : C

    fun <T> i1(t: T) where T : I1, T : I2
    fun <T> i2(t: T) where T : I2, T : I1
}

fun box(): String {
    val k = K::class.java

    k.getDeclaredMethod("c1", C::class.java)
    k.getDeclaredMethod("c2", C::class.java)
    k.getDeclaredMethod("c3", C::class.java)
    k.getDeclaredMethod("c4", C::class.java)

    k.getDeclaredMethod("i1", I1::class.java)
    k.getDeclaredMethod("i2", I2::class.java)

    return "OK"
}
