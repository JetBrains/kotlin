// TARGET_BACKEND: JVM

// WITH_REFLECT

import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KCallable
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.*
import kotlin.test.assertEquals

fun checkSuperAllSuperTypes(expected: List<KType>, actual: Collection<KType>) {
    assertEquals(expected.toSet(), actual.toSet())
}

fun checkSuperClasses(expected: List<KType>, actual: List<KClass<*>>) {
    assertEquals(expected.map { it.classifier as KClass<*> }, actual)
}

fun checkAllSuperClasses(expected: List<KType>, actual: Collection<KClass<*>>) {
    assertEquals(expected.map { it.classifier as KClass<*> }.toSet(), actual.toSet())
}

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
fun cloneable(): Cloneable = null!!

fun checkPrimitive(primitiveClass: KClass<*>): Unit {
    val parametrizedComparable = Comparable::class.createType(
        listOf(KTypeProjection(KVariance.INVARIANT, primitiveClass.starProjectedType))
    )
    val expectedSuperTypes = buildList {
        if (primitiveClass in listOf(Byte::class, Short::class, Int::class, Long::class, Float::class, Double::class)) {
            add(Number::class.starProjectedType)
        }
        add(parametrizedComparable)
        add(::serializable.returnType)
        if (primitiveClass in listOf(Boolean::class, Char::class)) {
            add(Any::class.starProjectedType)
        }
    }
    assertEquals(expectedSuperTypes, primitiveClass.supertypes)
    checkSuperClasses(expectedSuperTypes, primitiveClass.superclasses)

    val allExpectedSuperTypes = if (primitiveClass in listOf(Boolean::class, Char::class)) {
        expectedSuperTypes
    } else {
        expectedSuperTypes + Any::class.starProjectedType
    }
    checkSuperAllSuperTypes(allExpectedSuperTypes, primitiveClass.allSupertypes)
    checkAllSuperClasses(allExpectedSuperTypes, primitiveClass.allSuperclasses)
}

fun box(): String {
    check<Any>()
    checkAll<Any>()

    check<String>(::comparableOfString, ::charSequence, ::serializable, ::any)
    checkAll<String>(::comparableOfString, ::charSequence, ::serializable, ::any)

    // Primitives
    // Note that we can't use check/checkAll since primitive reified types will be wrapped after inlining

    checkPrimitive(Int::class)
    checkPrimitive(Byte::class)
    checkPrimitive(Short::class)
    checkPrimitive(Long::class)
    checkPrimitive(Float::class)
    checkPrimitive(Double::class)
    checkPrimitive(Boolean::class)
    checkPrimitive(Char::class)

    // End of primitives

    check<Array<Any>>(::any, ::cloneable, ::serializable)
    checkAll<Array<Any>>(::any, ::cloneable, ::serializable)

    check<DoubleArray>(::any, ::cloneable, ::serializable)
    checkAll<DoubleArray>(::any, ::cloneable, ::serializable)

    return "OK"
}
