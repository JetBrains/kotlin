// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.full.allSupertypes
import kotlin.test.assertEquals

interface A<A1, A2>
interface B<B1, B2> : A<B2, B1>
interface C<C1> : B<C1, String>
interface D : C<Int>

interface StringList : List<String>

fun box(): String {
    assertEquals(
            listOf(String::class, Int::class),
            D::class.allSupertypes.single { it.classifier == A::class }.arguments.map { it.type!!.classifier }
    )

    val collectionType = StringList::class.allSupertypes.single { it.classifier == Collection::class }
    val arg = collectionType.arguments.single().type!!
    // TODO: this does not work currently because for some reason two different instances of TypeParameterDescriptor are created for List
    // assertEquals(String::class, arg.classifier)

    return "OK"
}
