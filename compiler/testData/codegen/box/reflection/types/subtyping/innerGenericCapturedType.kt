// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.test.assertFalse
import kotlin.test.assertTrue

open class A<T1> {
    open inner class Inv<T2>
    open inner class In<in T3>
    open inner class Out<out T4>
}

class B<U1> : A<U1>() {
    inner class InvImpl<U2> : Inv<U2>()
    inner class InImpl<U3> : In<U3>()
    inner class OutImpl<U4> : Out<U4>()
}

fun invStarStar(): A<*>.Inv<*> = null!!
fun inStarStar(): A<*>.In<*> = null!!
fun outStarStar(): A<*>.Out<*> = null!!
fun invAnyAny(): A<Any>.Inv<Any> = null!!
fun inAnyAny(): A<Any>.In<Any> = null!!
fun outAnyAny(): A<Any>.Out<Any> = null!!
fun inInStringString(): A<in String>.In<String> = null!!

fun invImplStarStar(): B<*>.InvImpl<*> = null!!
fun inImplStarStar(): B<*>.InImpl<*> = null!!
fun outImplStarStar(): B<*>.OutImpl<*> = null!!
fun invImplAnyInt(): B<Any>.InvImpl<Int> = null!!
fun inImplAnyInt(): B<Any>.InImpl<Int> = null!!
fun outImplAnyInt(): B<Any>.OutImpl<Int> = null!!
fun outImplStringInt(): B<String>.OutImpl<Int> = null!!
fun inImplCharSequenceCharSequence(): B<CharSequence>.InImpl<CharSequence> = null!!

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

fun box(): String {
    checkStrictSubtype(::invImplStarStar, ::invStarStar)
    checkStrictSubtype(::inImplStarStar, ::inStarStar)
    checkStrictSubtype(::outImplStarStar, ::outStarStar)

    checkUnrelatedTypes(::invImplAnyInt, ::invAnyAny)
    checkUnrelatedTypes(::inImplAnyInt, ::inAnyAny)
    checkStrictSubtype(::outImplAnyInt, ::outAnyAny)
    checkUnrelatedTypes(::outImplStringInt, ::outAnyAny)
    checkStrictSubtype(::inImplCharSequenceCharSequence, ::inInStringString)

    return "OK"
}
