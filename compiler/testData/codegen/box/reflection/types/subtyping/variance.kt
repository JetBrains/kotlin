// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Inv<TInv>
class In<in TIn>
class Out<out TOut>

interface A<S, T, U : S> {
    fun inv_s(): Inv<S>
    fun inv_out_s(): Inv<out S>
    fun inv_in_s(): Inv<in S>
    fun inv_t(): Inv<T>
    fun inv_out_t(): Inv<out T>
    fun inv_in_t(): Inv<in T>
    fun inv_u(): Inv<U>
    fun inv_out_u(): Inv<out U>
    fun inv_in_u(): Inv<in U>

    fun in_s(): In<S>
    fun out_s(): Out<S>
    fun in_t(): In<T>
    fun out_t(): Out<T>
    fun in_u(): In<U>
    fun out_u(): Out<U>
}

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

fun checkUnrelatedTypes(type1: KCallable<*>, type2: KCallable<*>) {
    checkNotSubtype(type1, type2)
    checkNotSubtype(type2, type1)
}

typealias I = A<Any, Any, Any>

fun box(): String {
    // Inv

    checkStrictSubtype(I::inv_s, I::inv_out_s)
    checkStrictSubtype(I::inv_s, I::inv_in_s)
    checkUnrelatedTypes(I::inv_in_s, I::inv_out_s)

    for (s in listOf(I::inv_s, I::inv_out_s, I::inv_in_s)) {
        for (t in listOf(I::inv_t, I::inv_out_t, I::inv_in_t)) {
            checkUnrelatedTypes(s, t)
        }
    }

    checkUnrelatedTypes(I::inv_s, I::inv_u)
    checkUnrelatedTypes(I::inv_s, I::inv_out_u)
    checkStrictSubtype(I::inv_s, I::inv_in_u)
    checkStrictSubtype(I::inv_u, I::inv_out_s)
    checkStrictSubtype(I::inv_out_u, I::inv_out_s)
    checkUnrelatedTypes(I::inv_out_s, I::inv_in_u)

    // In / Out

    checkUnrelatedTypes(I::in_s, I::in_t)
    checkUnrelatedTypes(I::in_s, I::out_t)
    checkUnrelatedTypes(I::out_s, I::in_t)
    checkUnrelatedTypes(I::out_s, I::out_t)
    checkStrictSubtype(I::in_s, I::in_u)
    checkUnrelatedTypes(I::in_s, I::out_u)
    checkUnrelatedTypes(I::out_s, I::in_u)
    checkStrictSubtype(I::out_u, I::out_s)

    return "OK"
}
