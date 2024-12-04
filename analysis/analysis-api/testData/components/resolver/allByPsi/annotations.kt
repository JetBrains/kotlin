// WITH_REFLECT
package one

import kotlin.reflect.KClass

@Repeatable
annotation class AnnoWithArray(val arr: IntArray)

@Repeatable
annotation class AnnoWithKClass(val k: KClass<*>)

@Repeatable
annotation class AnnoWithArrayOfKClass(val a: Array<KClass<*>>)

annotation class AnnoWithAnotherAnnotation(val another: Array<AnnoWithArray>)

annotation class AnnoWithString(val str: String)

const val stringConstant = "s"

@AnnoWithArray([1, 2, 3])
@AnnoWithArray(arr = [4, 5, 6])
@AnnoWithArray(intArrayOf(7, 8, 9))
@AnnoWithKClass(AnnoWithKClass::class)
@[AnnoWithKClass(one.AnnoWithKClass::class)]
@AnnoWithArrayOfKClass([AnnoWithKClass::class, one.AnnoWithKClass::class])
@AnnoWithAnotherAnnotation(arrayOf(one.AnnoWithArray([10, 11, 12]), AnnoWithArray(intArrayOf(13, 14, 15))))
@AnnoWithString(stringConstant + " 1.2.3")
class A