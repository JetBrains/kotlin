// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.full.*
import kotlin.test.assertEquals

open class Simple
class OneClass : Simple()

interface Interface
interface Interface2
class ClassAndTwoInterfaces : Interface, Simple(), Interface2

class ClassWithSuperInterfaceOnly : Interface

annotation class AnnotationClass

fun any(): Any = null!!
fun simple(): Simple = null!!
fun interface_(): Interface = null!!
fun interface2(): Interface2 = null!!
fun annotation(): Annotation = null!!

fun box(): String {
    with(Simple::class) {
        assertEquals(listOf(::any.returnType), supertypes)
        assertEquals(listOf(Any::class), superclasses)
        // Calling toSet because the order of returned types/classes is not specified
        assertEquals(setOf(::any.returnType), allSupertypes.toSet())
        assertEquals(setOf(Any::class), allSuperclasses.toSet())
    }

    with (OneClass::class) {
        assertEquals(listOf(::simple.returnType), supertypes)
        assertEquals(listOf(Simple::class), superclasses)
        assertEquals(setOf(::simple.returnType, ::any.returnType), allSupertypes.toSet())
        assertEquals(setOf(Simple::class, Any::class), allSuperclasses.toSet())
    }

    with (Interface::class) {
        assertEquals(listOf(::any.returnType), supertypes)
        assertEquals(listOf(Any::class), superclasses)
        assertEquals(setOf(::any.returnType), allSupertypes.toSet())
        assertEquals(setOf(Any::class), allSuperclasses.toSet())
    }

    with (ClassAndTwoInterfaces::class) {
        assertEquals(listOf(::interface_.returnType, ::simple.returnType, ::interface2.returnType), supertypes)
        assertEquals(listOf(Interface::class, Simple::class, Interface2::class), superclasses)
        assertEquals(setOf(::interface_.returnType, ::simple.returnType, ::interface2.returnType, ::any.returnType), allSupertypes.toSet())
        assertEquals(setOf(Interface::class, Simple::class, Interface2::class, Any::class), allSuperclasses.toSet())
    }

    with (ClassWithSuperInterfaceOnly::class) {
        assertEquals(listOf(::interface_.returnType, ::any.returnType), supertypes)
        assertEquals(listOf(Interface::class, Any::class), superclasses)
        assertEquals(setOf(::interface_.returnType, ::any.returnType), allSupertypes.toSet())
        assertEquals(setOf(Interface::class, Any::class), allSuperclasses.toSet())
    }

    with (AnnotationClass::class) {
        assertEquals(listOf(::annotation.returnType, ::any.returnType), supertypes)
        assertEquals(listOf(Annotation::class, Any::class), superclasses)
        assertEquals(listOf(::annotation.returnType, ::any.returnType), allSupertypes)
        assertEquals(listOf(Annotation::class, Any::class), allSuperclasses)
    }

    return "OK"
}
