// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
import java.util.List;

public interface J {
    List rawList();
}

// FILE: box.kt
import kotlin.reflect.KCallable
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.test.assertFalse
import kotlin.test.assertTrue

fun listStar(): List<*> = null!!
fun listAny(): List<Any> = null!!
fun listNumber(): List<Number> = null!!
fun mutableListStar(): MutableList<*> = null!!
fun mutableListAny(): MutableList<Any> = null!!
fun mutableListInAny(): MutableList<in Any> = null!!
fun mutableListOutAny(): MutableList<out Any> = null!!
fun mutableListNumber(): MutableList<Number> = null!!
fun mutableListInNumber(): MutableList<in Number> = null!!
fun mutableListOutNumber(): MutableList<out Number> = null!!

fun checkSubtype(subtype: KCallable<*>, supertype: KCallable<*>) {
    assertTrue(subtype.returnType.isSubtypeOf(supertype.returnType), "Expected $subtype to be a subtype of $supertype")
    assertTrue(supertype.returnType.isSupertypeOf(subtype.returnType), "Expected $subtype to be a subtype of $supertype")
}

fun checkNotSubtype(subtype: KCallable<*>, supertype: KCallable<*>) {
    assertFalse(subtype.returnType.isSubtypeOf(supertype.returnType), "Expected $subtype NOT to be a subtype of $supertype")
    assertFalse(supertype.returnType.isSupertypeOf(subtype.returnType), "Expected $subtype NOT to be a subtype of $supertype")
}

fun checkStrictSubtype(subtype: KCallable<*>, supertype: KCallable<*>) {
    checkSubtype(subtype, supertype)
    checkNotSubtype(supertype, subtype)
}

fun checkEqualTypes(type1: KCallable<*>, type2: KCallable<*>) {
    checkSubtype(type1, type2)
    checkSubtype(type2, type1)
}

fun box(): String {
    checkEqualTypes(::listStar, J::rawList)
    checkStrictSubtype(::listAny, J::rawList)
    checkStrictSubtype(::listNumber, J::rawList)
    checkEqualTypes(::mutableListStar, J::rawList)
    checkStrictSubtype(::mutableListAny, J::rawList)
    checkEqualTypes(::mutableListInAny, J::rawList)
    checkStrictSubtype(::mutableListOutAny, J::rawList)
    checkStrictSubtype(::mutableListNumber, J::rawList)
    checkEqualTypes(::mutableListInNumber, J::rawList)
    checkStrictSubtype(::mutableListOutNumber, J::rawList)

    return "OK"
}
