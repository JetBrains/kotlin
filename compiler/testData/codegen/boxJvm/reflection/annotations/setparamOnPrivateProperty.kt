// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty
import kotlin.test.assertEquals

annotation class Ann

class C {
    @setparam:Ann
    private var defaultSetter = ""

    @setparam:Ann
    private var customSetter = ""
        set(value) {
            field = value
        }

    fun unboundDefaultSetter(): KMutableProperty<*> = C::defaultSetter
    fun boundDefaultSetter(): KMutableProperty<*> = this::defaultSetter

    fun unboundCustomSetter(): KMutableProperty<*> = C::customSetter
    fun boundCustomSetter(): KMutableProperty<*> = this::customSetter
}

fun setterParameterAnnotations(property: KCallable<*>): String =
    (property as KMutableProperty<*>).setter.parameters.last().annotations.toString()

fun box(): String {
    val c = C()

    assertEquals("[]", setterParameterAnnotations(c.unboundDefaultSetter()))
    assertEquals("[]", setterParameterAnnotations(c.boundDefaultSetter()))
    assertEquals("[]", setterParameterAnnotations(C::class.members.single { it.name == "defaultSetter" }))

    assertEquals("[@test.Ann()]", setterParameterAnnotations(c.unboundCustomSetter()))
    assertEquals("[@test.Ann()]", setterParameterAnnotations(c.boundCustomSetter()))
    assertEquals("[@test.Ann()]", setterParameterAnnotations(C::class.members.single { it.name == "customSetter" }))

    return "OK"
}
