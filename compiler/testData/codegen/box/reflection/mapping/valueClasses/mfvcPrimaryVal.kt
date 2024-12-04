// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses

import kotlin.reflect.jvm.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@JvmInline
value class Z1(val publicX1: UInt, val publicX2: Int) {
    companion object {
        val publicX1Ref = Z1::publicX1
        val publicX2Ref = Z1::publicX2
        val publicX1BoundRef = Z1(42U, -42)::publicX1
        val publicX2BoundRef = Z1(42U, -42)::publicX2
    }
}

@JvmInline
value class Z2(internal val internalX1: UInt, internal val internalX2: Int) {
    companion object {
        val internalX1Ref = Z2::internalX1
        val internalX2Ref = Z2::internalX2
        val internalX1BoundRef = Z2(42U, -42)::internalX1
        val internalX2BoundRef = Z2(42U, -42)::internalX2
    }
}

@JvmInline
value class Z3(private val privateX1: UInt, private val privateX2: Int) {
    companion object {
        val privateX1Ref = Z3::privateX1
        val privateX2Ref = Z3::privateX2
        val privateX1BoundRef = Z3(42U, -42)::privateX1
        val privateX2BoundRef = Z3(42U, -42)::privateX2
    }
}
@JvmInline
value class Z1_2(val publicX: Z1) {
    companion object {
        val publicXRef = Z1_2::publicX
        val publicXBoundRef = Z1_2(Z1(42U, -42))::publicX
    }
}

@JvmInline
value class Z2_2(internal val internalX: Z2) {
    companion object {
        val internalXRef = Z2_2::internalX
        val internalXBoundRef = Z2_2(Z2(42U, -42))::internalX
    }
}

@JvmInline
value class Z3_2(private val privateX: Z3) {
    companion object {
        val privateXRef = Z3_2::privateX
        val privateXBoundRef = Z3_2(Z3(42U, -42))::privateX
    }
}

fun box(): String {
    val suffix = "-pVg5ArA"
    assertEquals("getPublicX1$suffix", Z1.publicX1Ref.javaGetter!!.name)
    assertEquals("getPublicX2", Z1.publicX2Ref.javaGetter!!.name)
    assertEquals("getPublicX1$suffix", Z1.publicX1BoundRef.javaGetter!!.name)
    assertEquals("getPublicX2", Z1.publicX2BoundRef.javaGetter!!.name)

    assertTrue(Z2.internalX1Ref.javaGetter!!.name.startsWith("getInternalX1$suffix\$"), Z2.internalX1Ref.javaGetter!!.name)
    assertTrue(Z2.internalX2Ref.javaGetter!!.name.startsWith("getInternalX2\$"), Z2.internalX2Ref.javaGetter!!.name)
    assertTrue(Z2.internalX1BoundRef.javaGetter!!.name.startsWith("getInternalX1$suffix\$"), Z2.internalX1BoundRef.javaGetter!!.name)
    assertTrue(Z2.internalX2BoundRef.javaGetter!!.name.startsWith("getInternalX2\$"), Z2.internalX2BoundRef.javaGetter!!.name)

    assertEquals(null, Z3.privateX1Ref.javaGetter)
    assertEquals(null, Z3.privateX2Ref.javaGetter)
    assertEquals(null, Z3.privateX1BoundRef.javaGetter)
    assertEquals(null, Z3.privateX2BoundRef.javaGetter)
    
    
    assertEquals("getPublicX", Z1_2.publicXRef.javaGetter!!.name)
    assertEquals("getPublicX", Z1_2.publicXBoundRef.javaGetter!!.name)

    assertTrue(Z2_2.internalXRef.javaGetter!!.name.startsWith("getInternalX\$"), Z2_2.internalXRef.javaGetter!!.name)
    assertTrue(Z2_2.internalXBoundRef.javaGetter!!.name.startsWith("getInternalX\$"), Z2_2.internalXBoundRef.javaGetter!!.name)

    assertEquals("getPrivateX", Z3_2.privateXRef.javaGetter!!.name)
    assertEquals("getPrivateX", Z3_2.privateXBoundRef.javaGetter!!.name)

    return "OK"
}