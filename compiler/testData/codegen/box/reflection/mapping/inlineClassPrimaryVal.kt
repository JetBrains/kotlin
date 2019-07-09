// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

inline class Z1(val publicX: Int) {
    companion object {
        val publicXRef = Z1::publicX
        val publicXBoundRef = Z1(42)::publicX
    }
}

inline class Z2(internal val internalX: Int) {
    companion object {
        val internalXRef = Z2::internalX
        val internalXBoundRef = Z2(42)::internalX
    }
}

inline class Z3(private val privateX: Int) {
    companion object {
        val privateXRef = Z3::privateX
        val privateXBoundRef = Z3(42)::privateX
    }
}

fun box(): String {
    assertEquals("getPublicX", Z1.publicXRef.javaGetter!!.name)
    assertEquals("getPublicX", Z1.publicXBoundRef.javaGetter!!.name)

    assertEquals(null, Z2.internalXRef.javaGetter)
    assertEquals(null, Z2.internalXBoundRef.javaGetter)

    assertEquals(null, Z3.privateXRef.javaGetter)
    assertEquals(null, Z3.privateXBoundRef.javaGetter)

    return "OK"
}