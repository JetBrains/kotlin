// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class)

class C {
    inner class A(
        val a : Int = 1,
        @IntroducedAt("1") val b: String = "",
        @IntroducedAt("1") private val b1: String = "",
        @IntroducedAt("2") val c: Float = 3f,
    ) {
        override fun equals(other: Any?): Boolean {
            if (other !is A) return false
            return (this.a == other.a && this.b == other.b && this.b1 == other.b1 && this.c == other.c)
        }
    }
}

fun box(): String {
    val c = C()
    val cA = c.A()

    val constructor1 = C.A::class.java.getConstructor(C::class.java, Int::class.java)
    val constructor2 = C.A::class.java.getConstructor(C::class.java, Int::class.java, String::class.java, String::class.java)

    val r1 = constructor1.newInstance(c, cA.a) as C.A
    val r2 = constructor2.newInstance(c, cA.a, cA.b, "") as C.A

    return if ((r1 == cA) && (r2 == cA)) "OK" else "Err1: $cA $r1 $r2 "
}