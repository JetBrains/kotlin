// LANGUAGE: +ContextParameters
// OPT_IN: kotlin.ExperimentalContextParameters
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.jvm.internal.Reflection
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.test.assertEquals

context(c: Int) val simple: String
    get() = "simple$c"

var storage = "initial"

context(c: Int, s: String) var Boolean.mutable: String
    get() = storage + c + s + this
    set(value) {
        storage = value + c + s + this
    }

context(c: Int) val String.extension: String
    get() = this + c

fun box(): String {
    val members = Reflection.getOrCreateKotlinPackage(object {}::class.java.enclosingClass).members

    val simpleProperty = members.single { it.name == "simple" } as KProperty<*>
    assertEquals(listOf(KParameter.Kind.CONTEXT), simpleProperty.parameters.map { it.kind })
    assertEquals("simple1", simpleProperty.call(1))
    assertEquals("simple2", simpleProperty.getter.call(2))

    // A 3-argument getter and 4-argument setter: an arity no fixed KProperty0/1/2 shape can represent, only the N-ary one.
    val mutableProperty = members.single { it.name == "mutable" } as KMutableProperty<*>
    assertEquals(
        listOf(KParameter.Kind.CONTEXT, KParameter.Kind.CONTEXT, KParameter.Kind.EXTENSION_RECEIVER),
        mutableProperty.parameters.map { it.kind },
    )
    assertEquals("initial3xtrue", mutableProperty.getter.call(3, "x", true))
    mutableProperty.setter.call(4, "y", false, "updated")
    assertEquals("updated4yfalse", storage)

    val extensionProperty = members.single { it.name == "extension" } as KProperty<*>
    assertEquals(
        listOf(KParameter.Kind.CONTEXT, KParameter.Kind.EXTENSION_RECEIVER),
        extensionProperty.parameters.map { it.kind },
    )
    assertEquals("OK5", extensionProperty.getter.call(5, "OK"))

    return "OK"
}
