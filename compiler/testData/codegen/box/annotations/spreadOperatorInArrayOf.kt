// ISSUE: KT-86481
// WITH_STDLIB

@file:OptIn(ExperimentalUnsignedTypes::class)

enum class E { X }

annotation class StringAnno(val v: Array<String> = arrayOf(*["!"]))
annotation class EnumAnno(vararg val v: E = arrayOf(*arrayOf(E.X, *[E.X]), E.X))
annotation class IntAnno(vararg val v: Int = intArrayOf(1, *intArrayOf(2, *intArrayOf(3))))
annotation class UIntAnno(val v: UIntArray = uintArrayOf(1u, *uintArrayOf(2u), 3u))

@StringAnno(arrayOf(*["!"], "!", *arrayOf(elements = arrayOf("!"))))
@EnumAnno(*arrayOf(*arrayOf(*emptyArray())), E.X, *arrayOf(elements = arrayOf(*arrayOf(*arrayOf(*[E.X])))))
@IntAnno(v = intArrayOf(*intArrayOf(1), *intArrayOf(2), *intArrayOf(3)))
@UIntAnno(v = uintArrayOf(elements = uintArrayOf(1u, 2u, 3u)))
fun target(): String = "OK"

fun box() = target()
