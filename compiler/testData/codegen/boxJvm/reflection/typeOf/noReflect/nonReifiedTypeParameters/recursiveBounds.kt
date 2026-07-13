// TARGET_BACKEND: JVM
// WITH_STDLIB
// LANGUAGE: +JvmSupportRecursiveTypeOf

package test

import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

val KTypeParameter.upperBound: KType get() = upperBounds.single()
val KType.argumentType: KType get() = arguments.single().type!!
val KType.asTp: KTypeParameter get() = classifier as KTypeParameter

fun <T: Comparable<T>> selfRecursive(): KTypeParameter =
    typeOf<List<T>>().argumentType.asTp

class C<T: Comparable<U>, U: Comparable<T>> {
    fun makeTU(): List<KTypeParameter> =
        typeOf<Pair<T, U>>().arguments.map { it.type!!.asTp }
}

fun <A: Comparable<B>, B: Comparable<C>, C: Comparable<A>> threeWayCycle(): List<KTypeParameter> =
    typeOf<Triple<A, B, C>>().arguments.map { it.type!!.asTp }

fun <A, T: Comparable<T>> repeatedNonCyclic(): List<KTypeParameter> =
    typeOf<Triple<A, T, A>>().arguments.map { it.type!!.asTp }

inline fun <reified T: Comparable<T>, U: T> reifiedRecursive(): KTypeParameter =
    typeOf<Pair<T, U>>().arguments[1].type!!.asTp

inline fun <reified R, T: Comparable<T>> reifiedMixedWithRecursive(): List<KType?> =
    typeOf<Triple<R, T, R>>().arguments.map { it.type }

fun box(): String {
    run {
        val t = selfRecursive<Int>()
        assertEquals("java.lang.Comparable<T> (Kotlin reflection is not available)", t.upperBound.toString())
        assertEquals(t, t.upperBound.argumentType.asTp)
    }

    run {
        val (t, u) = C<Int, Int>().makeTU()
        assertEquals("java.lang.Comparable<U> (Kotlin reflection is not available)", t.upperBound.toString())
        assertEquals("java.lang.Comparable<T> (Kotlin reflection is not available)", u.upperBound.toString())
        assertEquals(u, t.upperBound.argumentType.asTp)
        assertEquals(t, u.upperBound.argumentType.asTp)
    }

    run {
        val (a, b, c) = threeWayCycle<Int, Int, Int>()
        assertEquals(b, a.upperBound.argumentType.asTp)
        assertEquals(c, b.upperBound.argumentType.asTp)
        assertEquals(a, c.upperBound.argumentType.asTp)
    }

    run {
        val (a0, t, a1) = repeatedNonCyclic<Any, Int>()
        assertEquals(a0, a1)
        assertEquals("java.lang.Object? (Kotlin reflection is not available)", a0.upperBound.toString())
        assertEquals("java.lang.Comparable<T> (Kotlin reflection is not available)", t.upperBound.toString())
        assertEquals(t, t.upperBound.argumentType.asTp)
    }

    run {
        val u = reifiedRecursive<Int, Int>()
        assertEquals("U", u.toString())
        val t = u.upperBound.asTp
        assertEquals("T", t.toString())
        assertEquals("java.lang.Comparable<T> (Kotlin reflection is not available)", t.upperBound.toString())
        assertEquals(t, t.upperBound.argumentType.asTp)
    }

    run {
        val args = reifiedMixedWithRecursive<String, Int>()
        assertEquals("java.lang.String (Kotlin reflection is not available)", args[0].toString())
        assertEquals("java.lang.String (Kotlin reflection is not available)", args[2].toString())
        val t = args[1]!!.asTp
        assertEquals("java.lang.Comparable<T> (Kotlin reflection is not available)", t.upperBound.toString())
        assertEquals(t, t.upperBound.argumentType.asTp)
    }

    return "OK"
}
