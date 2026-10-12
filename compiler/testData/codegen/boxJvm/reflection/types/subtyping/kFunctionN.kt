// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class K {
    fun kf0(): KFunction0<Int> = null!!
    fun kf1(): KFunction1<Int, Int> = null!!
    fun kf2(): KFunction2<Int, String, Int> = null!!
    fun ksf1(): KSuspendFunction1<Int, Int> = null!!
    fun kfString(): KFunction<String> = null!!
    fun kCallable(): KCallable<Int> = null!!
    fun function(): Function<Int> = null!!
    fun any(): Any = null!!
    fun string(): String = ""
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

fun box(): String {
    checkStrictSubtype(K::kf1, K::function)
    checkUnrelatedTypes(K::kf1, K::string)
    checkUnrelatedTypes(K::function, K::string)

    for (kFunctionN in listOf(K::kf0, K::kf1, K::kf2, K::ksf1)) {
        checkStrictSubtype(kFunctionN, K::function)
        checkStrictSubtype(kFunctionN, K::kCallable)
        checkStrictSubtype(kFunctionN, K::any)
        checkUnrelatedTypes(kFunctionN, K::kfString)
        checkUnrelatedTypes(kFunctionN, K::string)
    }

    return "OK"
}
