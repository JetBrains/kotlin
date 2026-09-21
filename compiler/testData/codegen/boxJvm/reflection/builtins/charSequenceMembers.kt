// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK

import kotlin.reflect.*
import kotlin.reflect.jvm.javaMethod
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class A : CharSequence

class B : A() {
    override val length: Int get() = 2
    override fun get(index: Int): Char = "OK"[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = "OK".subSequence(startIndex, endIndex)
}

fun checkMembers(klass: KClass<*>) {
    val members = klass.members
    assertTrue(members.none { it.name == "charAt" }, "$klass: $members")
    assertTrue(members.single { it.name == "get" } is KFunction<*>, "$klass: $members")
    assertTrue(members.single { it.name == "length" } is KProperty<*>, "$klass: $members")
}

fun box(): String {
    checkMembers(CharSequence::class)
    checkMembers(A::class)
    checkMembers(B::class)

    assertEquals('O', B::get.call(B(), 0))
    assertEquals(2, CharSequence::length.call("OK"))
    assertEquals(2, A::length.call(B()))
    assertEquals("get", B::get.javaMethod!!.name)

    assertEquals('K', B::class.members.single { it.name == "get" }.call(B(), 1))
    assertEquals(2, CharSequence::class.members.single { it.name == "length" }.call("OK"))

    // K1-based implementation cannot resolve `CharSequence.get` because `RuntimeTypeMapper.mapSignature` computes the JVM signature
    // incorrectly: `get(I)C` instead of `charAt(I)C`.
    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) != true &&
            Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1ImplementationForMembers").invoke(null) != true) {
        assertEquals('K', CharSequence::get.call("OK", 1))
        assertEquals('K', A::get.call(B(), 1))

        assertEquals("charAt", CharSequence::get.javaMethod!!.name)
        assertEquals("charAt", A::get.javaMethod!!.name)

        assertEquals('O', CharSequence::class.members.single { it.name == "get" }.call("OK", 0))
        assertEquals('K', A::class.members.single { it.name == "get" }.call(B(), 1))
    }

    return "OK"
}
