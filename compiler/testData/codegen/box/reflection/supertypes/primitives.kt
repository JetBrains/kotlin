// WITH_REFLECT

import kotlin.reflect.isSubclassOf
import kotlin.test.assertTrue
import kotlin.test.assertFalse

fun box(): String {
    // KClass instances for primitive int and wrapper java.lang.Integer are different
    val primitiveInt = Int::class.javaPrimitiveType!!.kotlin
    val wrapperInt = Int::class.javaObjectType.kotlin
    assertTrue(primitiveInt.isSubclassOf(primitiveInt))
    assertTrue(wrapperInt.isSubclassOf(wrapperInt))

    // Currently KClass for int != KClass for java.lang.Integer, thus they are not a subclass of each other either
    // TODO: reconsider this decision
    assertFalse(primitiveInt.isSubclassOf(wrapperInt))
    assertFalse(wrapperInt.isSubclassOf(primitiveInt))

    return "OK"
}
