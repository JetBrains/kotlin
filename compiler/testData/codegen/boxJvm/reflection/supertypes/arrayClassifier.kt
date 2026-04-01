// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KClass
import kotlin.test.assertEquals

interface A<T>
class ArrayAny : A<Array<Any>>
class ArrayString : A<Array<String>>
class ArrayArrayIntArray : A<Array<Array<IntArray>>>
class ArrayStar : A<Array<*>>
class ArrayTypeParameter<U> : A<Array<U>>
class LongArray0 : A<LongArray>

fun check(expectedClassifier: KClass<*>, test: KClass<*>) {
    val actual = test.supertypes.single { it.classifier == A::class }.arguments.single().type!!.classifier
    assertEquals(expectedClassifier, actual)
}

fun box(): String {
    check(Array<Any>::class, ArrayAny::class)
    check(Array<String>::class, ArrayString::class)
    check(Array<Array<IntArray>>::class, ArrayArrayIntArray::class)
    check(Array<Any>::class, ArrayStar::class)
    check(Array<Any>::class, ArrayTypeParameter::class)
    check(LongArray::class, LongArray0::class)

    return "OK"
}
