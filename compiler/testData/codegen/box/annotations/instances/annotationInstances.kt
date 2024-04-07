// IGNORE_BACKEND: JVM

// (supported: JVM_IR, JS_IR(_ES6), NATIVE)
// Regular JS works too, but without proper hashCode or equals

// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

// note: taken from ../parameters.kt and ../parametersWithPrimitiveValues.kt
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue as assert

enum class E { E0 }
annotation class Empty

annotation class A(
    val b: Byte,
    val s: Short,
    val i: Int,
    val f: Float,
    val d: Double,
    val l: Long,
    val c: Char,
    val bool: Boolean
)

@Retention(AnnotationRetention.RUNTIME)
annotation class Anno(
    val s: String,
    val i: Int,
    val f: Double,
    val u: UInt,
    val e: E,
    val a: A,
    val k: KClass<*>,
    val arr: Array<String>,
    val intArr: IntArray,
    val arrOfE: Array<E>,
    val arrOfA: Array<Empty>,
    val arrOfK: Array<KClass<*>>
)


fun box(): String {
    val anno = Anno(
        "OK", 42, 2.718281828, 43u, E.E0,
        A(1, 1, 1, 1.0.toFloat(), 1.0, 1, 'c', true),
        A::class, emptyArray(), intArrayOf(1, 2), arrayOf(E.E0), arrayOf(Empty()), arrayOf(E::class, Empty::class)
    )
    assertEquals(anno.s, "OK")
    assertEquals(anno.i, 42)
    assert(anno.f > 2.0 && anno.f < 3.0)
    assertEquals(anno.u, 43u)
    assertEquals(anno.e, E.E0)
    assert(anno.a is A)
    assert(anno.k == A::class)
    assert(anno.arr.isEmpty())
    assert(anno.intArr.contentEquals(intArrayOf(1, 2)))
    assert(anno.arrOfE.contentEquals(arrayOf(E.E0)))
    assert(anno.arrOfA.size == 1)
    assert(anno.arrOfK.size == 2)
    val ann = anno.a
    assertEquals(ann.b, 1.toByte())
    assertEquals(ann.s, 1.toShort())
    assertEquals(ann.i, 1)
    assertEquals(ann.f, 1.toFloat())
    assertEquals(ann.d, 1.0)
    assertEquals(ann.l, 1.toLong())
    assertEquals(ann.c, 'c')
    assert(ann.bool)
    return "OK"
}
