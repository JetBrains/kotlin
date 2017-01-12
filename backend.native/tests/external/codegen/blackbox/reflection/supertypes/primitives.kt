// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.reflect.full.isSubclassOf
import kotlin.test.assertTrue
import kotlin.test.assertFalse

fun box(): String {
    // KClass instances for primitive int and wrapper java.lang.Integer are different
    val primitiveInt = Int::class.javaPrimitiveType!!.kotlin
    val wrapperInt = Int::class.javaObjectType.kotlin
    assertTrue(primitiveInt.isSubclassOf(primitiveInt))
    assertTrue(wrapperInt.isSubclassOf(wrapperInt))

    // KClass for int equals KClass for java.lang.Integer, so they are also a subclass of each other
    assertTrue(primitiveInt.isSubclassOf(wrapperInt))
    assertTrue(wrapperInt.isSubclassOf(primitiveInt))

    return "OK"
}
