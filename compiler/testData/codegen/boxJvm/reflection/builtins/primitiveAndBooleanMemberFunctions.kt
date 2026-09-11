// TARGET_BACKEND: JVM
// WITH_REFLECT
// Tests that Boolean and Char expose their full set of member functions via reflection.

import kotlin.reflect.full.*
import kotlin.test.*

fun box(): String {
    // Boolean has compareTo, and, or, xor, not, toInt, equals, hashCode, toString
    val boolFns = Boolean::class.memberFunctions.map { it.name }.toSet()
    assertTrue("compareTo" in boolFns, "Boolean must have compareTo()")
    assertTrue("and"       in boolFns, "Boolean must have and()")
    assertTrue("or"        in boolFns, "Boolean must have or()")
    assertTrue("not"       in boolFns, "Boolean must have not()")

    // Char has compareTo, plus(Int), minus(Char/Int), toInt/toLong/etc., and range operators
    val charFns = Char::class.memberFunctions.map { it.name }.toSet()
    assertTrue("compareTo" in charFns, "Char must have compareTo()")
    assertTrue("plus"      in charFns, "Char must have plus()")
    assertTrue("minus"     in charFns, "Char must have minus()")

    // Both must include Object methods
    for ((klass, fns) in listOf(Boolean::class to boolFns, Char::class to charFns)) {
        assertTrue("equals"   in fns, "${klass.simpleName} must have equals()")
        assertTrue("hashCode" in fns, "${klass.simpleName} must have hashCode()")
        assertTrue("toString" in fns, "${klass.simpleName} must have toString()")
    }

    return "OK"
}
