// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.test.assertFalse
import kotlin.test.assertTrue

interface A<T> {
    fun f(t: T & Any): T & Any
    fun g(): T & Any
    fun <T> h(): T & Any

    fun t(): T
}

fun checkSubtype(subtype: KType, supertype: KType) {
    assertTrue(subtype.isSubtypeOf(supertype), "Expected $subtype to be a subtype of $supertype")
    assertTrue(supertype.isSupertypeOf(subtype), "Expected $subtype to be a subtype of $supertype")
}

fun checkNotSubtype(subtype: KType, supertype: KType) {
    assertFalse(subtype.isSubtypeOf(supertype), "Expected $subtype NOT to be a subtype of $supertype")
    assertFalse(supertype.isSupertypeOf(subtype), "Expected $subtype NOT to be a subtype of $supertype")
}

fun checkEqualTypes(type1: KType, type2: KType) {
    assertTrue(type1.isSubtypeOf(type2), "Expected $type1 to be a subtype of $type2")
    assertTrue(type2.isSubtypeOf(type1), "Expected $type2 to be a subtype of $type1")
}

fun checkUnrelatedTypes(type1: KType, type2: KType) {
    assertFalse(type1.isSubtypeOf(type2), "Expected $type1 NOT to be a subtype of $type2")
    assertFalse(type2.isSubtypeOf(type1), "Expected $type2 NOT to be a subtype of $type1")
    assertFalse(type1.isSupertypeOf(type2), "Expected $type2 NOT to be a subtype of $type1")
    assertFalse(type2.isSupertypeOf(type1), "Expected $type1 NOT to be a subtype of $type2")
}

fun box(): String {
    val f = A<*>::f
    val t1 = f.parameters.last().type
    val t2 = f.returnType
    checkEqualTypes(t1, t2)

    val g = A<*>::f
    val t3 = g.returnType
    checkEqualTypes(t1, t3)

    val h = A::class.members.single { it.name == "h" }.returnType
    checkUnrelatedTypes(t1, h)

    val t = A<*>::t.returnType
    checkSubtype(t1, t)
    checkNotSubtype(t, t1)

    return "OK"
}
