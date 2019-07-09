// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.test.assertEquals

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
                    KTypeProjection.invariant(string),
                    KTypeProjection.contravariant(string),
                    KTypeProjection.covariant(string),
                    KTypeProjection.STAR
            ),
            ::projections.returnType.arguments
    )
    assertEquals(
            listOf(string, string, string, null),
            ::projections.returnType.arguments.map(KTypeProjection::type)
    )

    val outNumber = ::array.returnType.arguments.single()
    assertEquals(KVariance.OUT, outNumber.variance)
    assertEquals(Number::class, outNumber.type?.classifier)

    // There should be no use-site variance, despite the fact that the corresponding parameter has 'out' variance
    assertEquals(KVariance.INVARIANT, ::list.returnType.arguments.single().variance)

    return "OK"
}
