// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
public interface J {
    Number[] javaArrayNumber();
    Integer[] javaArrayInteger();
    int[] javaArrayInt();
}

// FILE: box.kt
import kotlin.reflect.KCallable
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.test.assertFalse
import kotlin.test.assertTrue

fun arrayNumber(): Array<Number> = null!!
fun arrayOutNumber(): Array<out Number> = null!!
fun arrayInNumber(): Array<in Number> = null!!
fun arrayInt(): Array<Int> = null!!
fun arrayOutInt(): Array<out Int> = null!!
fun arrayInInt(): Array<in Int> = null!!
fun arrayAny(): Array<Any> = null!!
fun arrayOutAny(): Array<out Any> = null!!
fun arrayInAny(): Array<in Any> = null!!
fun arrayStar(): Array<*> = null!!

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

fun checkUnrelatedTypes(type1: KCallable<*>, type2: KCallable<*>) {
    checkNotSubtype(type1, type2)
    checkNotSubtype(type2, type1)
}

fun box(): String {
    checkEqualTypes(::arrayNumber, J::javaArrayNumber)
    checkEqualTypes(::arrayOutNumber, J::javaArrayNumber)
    checkStrictSubtype(J::javaArrayNumber, ::arrayInNumber)
    checkStrictSubtype(::arrayInt, J::javaArrayNumber)
    checkStrictSubtype(::arrayOutInt, J::javaArrayNumber)
    checkStrictSubtype(J::javaArrayNumber, ::arrayInInt)
    checkUnrelatedTypes(::arrayAny, J::javaArrayNumber)
    checkStrictSubtype(J::javaArrayNumber, ::arrayOutAny)
    checkUnrelatedTypes(::arrayInAny, J::javaArrayNumber)
    checkStrictSubtype(J::javaArrayNumber, ::arrayStar)

    checkUnrelatedTypes(::arrayNumber, J::javaArrayInteger)
    checkStrictSubtype(J::javaArrayInteger, ::arrayOutNumber)
    checkUnrelatedTypes(::arrayInNumber, J::javaArrayInteger)
    checkEqualTypes(::arrayInt, J::javaArrayInteger)
    checkEqualTypes(::arrayOutInt, J::javaArrayInteger)
    checkStrictSubtype(J::javaArrayInteger, ::arrayInInt)
    checkUnrelatedTypes(::arrayAny, J::javaArrayInteger)
    checkStrictSubtype(J::javaArrayInteger, ::arrayOutAny)
    checkUnrelatedTypes(::arrayInAny, J::javaArrayInteger)
    checkStrictSubtype(J::javaArrayInteger, ::arrayStar)

    checkUnrelatedTypes(::arrayNumber, J::javaArrayInt)
    checkUnrelatedTypes(::arrayOutNumber, J::javaArrayInt)
    checkUnrelatedTypes(::arrayInNumber, J::javaArrayInt)
    checkUnrelatedTypes(::arrayInt, J::javaArrayInt)
    checkUnrelatedTypes(::arrayOutInt, J::javaArrayInt)
    checkUnrelatedTypes(::arrayInInt, J::javaArrayInt)
    checkUnrelatedTypes(::arrayAny, J::javaArrayInt)
    checkUnrelatedTypes(::arrayOutAny, J::javaArrayInt)
    checkUnrelatedTypes(::arrayInAny, J::javaArrayInt)
    checkUnrelatedTypes(::arrayStar, J::javaArrayInt)

    return "OK"
}
