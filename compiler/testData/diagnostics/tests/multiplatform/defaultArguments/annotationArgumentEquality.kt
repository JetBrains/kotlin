// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

import kotlin.reflect.KClass

expect annotation class Primitives(
    val z: Boolean = true,
    val c: Char = 'c',
    val b: Byte = 42.toByte(),
    val s: Short = (-1).toShort(),
    val i: Int = -42,
    val f: Float = 2.72f,
    val j: Long = 123456789123456789L,
    val d: Double = 3.14159265358979
)

expect annotation class PrimitiveArrays(
    val z: BooleanArray = [true],
    val c: CharArray = ['c'],
    val b: ByteArray = [42.toByte()],
    val s: ShortArray = [(-1).toShort()],
    val i: IntArray = [-42],
    val f: FloatArray = [2.72f],
    val j: LongArray = [123456789123456789L],
    val d: DoubleArray = [3.14159265358979]
)

enum class En { A, B }

annotation class Anno(val value: String = "Anno")

expect annotation class Classes(
    val s: String = "OK",
    val e: En = En.B,
    // TODO: this does not work at the moment because AnnotationDescriptor subclasses do not implement equals correctly
    // val a: Anno = Anno(),
    val k: KClass<*> = List::class
)

expect annotation class ClassArrays(
    val s: Array<String> = ["OK"],
    val e: Array<En> = [En.B],
    // val a: Array<Anno> = [Anno()],
    val k: Array<KClass<*>> = [List::class],
    vararg val v: Int = [42]
)

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

import kotlin.reflect.KClass

actual annotation class Primitives(
    actual val z: Boolean = true,
    actual val c: Char = 'c',
    actual val b: Byte = 42.toByte(),
    actual val s: Short = (-1).toShort(),
    actual val i: Int = -42,
    actual val f: Float = 2.72f,
    actual val j: Long = 123456789123456789L,
    actual val d: Double = 3.14159265358979
)

actual annotation class PrimitiveArrays(
    actual val z: BooleanArray = [true],
    actual val c: CharArray = ['c'],
    actual val b: ByteArray = [42.toByte()],
    actual val s: ShortArray = [(-1).toShort()],
    actual val i: IntArray = [-42],
    actual val f: FloatArray = [2.72f],
    actual val j: LongArray = [123456789123456789L],
    actual val d: DoubleArray = [3.14159265358979]
)

actual annotation class Classes(
    actual val s: String = "OK",
    actual val e: En = En.B,
    // actual val a: Anno = Anno(),
    actual val k: KClass<*> = List::class
)

actual annotation class ClassArrays(
    actual val s: Array<String> = ["OK"],
    actual val e: Array<En> = [En.B],
    // actual val a: Array<Anno> = [Anno()],
    actual val k: Array<KClass<*>> = [List::class],
    actual vararg val v: Int = [42]
)
