// WITH_REFLECT

import kotlin.reflect.KTypeProjection
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun string(): String = null!!

class Fourple<A, B, C, D>
fun projections(): Fourple<String, in String, out String, *> = null!!

fun array(): Array<out Number> = null!!

fun list(): List<String> = null!!

fun box(): String {
    val string = ::string.returnType
    assertEquals(listOf(), string.arguments)

    assertEquals(
            listOf(
                    KTypeProjection.Invariant(string),
                    KTypeProjection.In(string),
                    KTypeProjection.Out(string),
                    KTypeProjection.Star
            ),
            ::projections.returnType.arguments
    )
    assertEquals(
            listOf(string, string, string, null),
            ::projections.returnType.arguments.map(KTypeProjection::type)
    )

    val outNumber = ::array.returnType.arguments.single()
    assertTrue(outNumber is KTypeProjection.Out)
    assertEquals(Number::class, outNumber.type?.classifier)

    // There should be no use-site projection, despite the fact that the corresponding parameter has 'out' variance
    assertTrue(::list.returnType.arguments.single() is KTypeProjection.Invariant)

    return "OK"
}
