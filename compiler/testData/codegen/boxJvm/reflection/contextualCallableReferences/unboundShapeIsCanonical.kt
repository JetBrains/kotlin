// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// OPT_IN: kotlin.ExperimentalContextParameters
// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK
// ISSUE: KT-86452

import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.KProperty2
import kotlin.reflect.KTypeParameter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

context(c: String)
val <T> List<T>.head: T get() = first()

context(c: String)
var <T> MutableList<T>.headVar: T
    get() = first()
    set(value) { this[0] = value }

private fun KTypeParameter.owner(): Any {
    val field = javaClass.getDeclaredField("container").apply { isAccessible = true }
    return field.get(this)
}

private fun checkCanonical(bound: KProperty<*>, expectMutable: Boolean, description: String) {
    val owner = bound.typeParameters.single().owner()
    assertTrue(owner is KProperty<*>, "$description: owner is ${owner.javaClass.name}")
    assertTrue(owner !is KProperty0<*> && owner !is KProperty1<*, *> && owner !is KProperty2<*, *, *>, "$description: owner is ${owner.javaClass.name}")
    assertEquals(expectMutable, owner is KMutableProperty<*>, "$description: mutability")
    assertEquals(
        listOf(KParameter.Kind.CONTEXT, KParameter.Kind.EXTENSION_RECEIVER),
        (owner as KProperty<*>).parameters.map { it.kind },
        "$description: parameters",
    )
}

fun box(): String {
    context("ctx") {
        checkCanonical(List<Int>::head, expectMutable = false, "context-bound val")
        checkCanonical(MutableList<Int>::headVar, expectMutable = true, "context-bound var")

        checkCanonical(listOf(1)::head, expectMutable = false, "fully bound val")
        checkCanonical(mutableListOf(1)::headVar, expectMutable = true, "fully bound var")

        assertEquals(1, listOf(1)::head.get())
        val list = mutableListOf(1)
        (list::headVar).set(2)
        assertEquals(2, MutableList<Int>::headVar.get(list))
    }
    return "OK"
}
