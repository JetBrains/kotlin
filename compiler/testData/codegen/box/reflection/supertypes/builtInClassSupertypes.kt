// TARGET_BACKEND: JVM

// WITH_REFLECT

import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KCallable
import kotlin.reflect.full.*
import kotlin.test.assertEquals

inline fun <reified T : Any> check(vararg callables: KCallable<*>) {
    val types = callables.map { it.returnType }
    assertEquals(types, T::class.supertypes)
    assertEquals(types.map { it.classifier as KClass<*> }, T::class.superclasses)
}

inline fun <reified T : Any> checkAll(vararg callables: KCallable<*>) {
    val types = callables.map { it.returnType }
    // Calling toSet because the order of returned types/classes is not specified
    assertEquals(types.toSet(), T::class.allSupertypes.toSet())
    assertEquals(types.map { it.classifier as KClass<*> }.toSet(), T::class.allSuperclasses.toSet())
}

fun comparableOfString(): Comparable<String> = null!!
fun charSequence(): CharSequence = null!!
fun serializable(): Serializable = null!!
fun any(): Any = null!!
fun number(): Number = null!!
fun comparableOfInt(): Comparable<Int> = null!!
fun cloneable(): Cloneable = null!!

fun box(): String {
    check<Any>()
    checkAll<Any>()

    check<String>(::comparableOfString, ::charSequence, ::serializable, ::any)
    checkAll<String>(::comparableOfString, ::charSequence, ::serializable, ::any)

    check<Int>(::number, ::comparableOfInt, ::serializable)
    checkAll<Int>(::number, ::comparableOfInt, ::serializable, ::any)

    check<Array<Any>>(::any, ::cloneable, ::serializable)
    checkAll<Array<Any>>(::any, ::cloneable, ::serializable)

    return "OK"
}
