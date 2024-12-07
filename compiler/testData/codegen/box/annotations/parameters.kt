// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.reflect.KClass

enum class E { E0 }
annotation class A

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
    val arrOfA: Array<A>
)

@Anno("OK", 42, 2.718281828, 43u, E.E0, A(), A::class, emptyArray(), intArrayOf(1, 2), arrayOf(E.E0), arrayOf(A()))
class TTT

fun box(): String {
    val anno = TTT::class.annotations.single() as Anno
    assertEquals(anno.s, "OK")
    assertEquals(anno.i, 42)
    assert(anno.f > 2.0 && anno.f < 3.0)
    assertEquals(anno.u, 43u)
    assertEquals(anno.e, E.E0)
    assert(anno.a is A)
//  TODO: problems with KClass/Class conversion in JVM_IR, unrelated to annotation codegen
//    assert(anno.k == A::class.java)
    assert(anno.arr.isEmpty())
    assert(anno.intArr.contentEquals(intArrayOf(1, 2)))
    assert(anno.arrOfE.contentEquals(arrayOf(E.E0)))
    assert(anno.arrOfA.size == 1)
    return "OK"
}
