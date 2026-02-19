// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.reflect.full.allSupertypes
import kotlin.test.assertEquals

interface A<A1, A2>
interface B<B1, B2> : A<B2, B1>
interface C<C1> : B<C1, String>
interface D : C<Int>

interface StringList : List<String>

interface Projections : A<MutableMap<MutableList<in Number>, MutableList<out Number>>, MutableList<*>>

@Target(AnnotationTarget.TYPE)
annotation class Anno

class AnnotatedSupertype : B<@Anno Any, String>

fun box(): String {
    assertEquals(
        listOf(String::class, Int::class),
        D::class.allSupertypes.single { it.classifier == A::class }.arguments.map { it.type!!.classifier }
    )

    val collectionType = StringList::class.allSupertypes.single { it.classifier == Collection::class }
    val arg = collectionType.arguments.single().type!!
    assertEquals(String::class, arg.classifier)

    assertEquals(
        "[test.A<kotlin.collections.MutableMap<kotlin.collections.MutableList<in kotlin.Number>, kotlin.collections.MutableList<out kotlin.Number>>, kotlin.collections.MutableList<*>>, kotlin.Any]",
        Projections::class.allSupertypes.toString(),
    )

    // TODO (KT-77700): no annotations on the supertypes.
    assertEquals(
        "[test.B<kotlin.Any, kotlin.String>, test.A<kotlin.String, kotlin.Any>, kotlin.Any]",
        AnnotatedSupertype::class.allSupertypes.toString(),
    )

    return "OK"
}
