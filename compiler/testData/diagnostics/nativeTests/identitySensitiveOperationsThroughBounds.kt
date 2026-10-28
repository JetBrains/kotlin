// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +FullValueClasses
// OPT_IN: kotlin.ExperimentalValueClassesApi
@file:OptIn(ExperimentalNativeApi::class)

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode
import kotlin.native.ref.WeakReference

abstract value class AbstractValue

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"
}

fun <T : AbstractValue> throughBounds(t: T) {
    t.identityHashCode()
    WeakReference(t)
}

fun willBecomeValue(wrapper: Wrapper) {
    WeakReference(wrapper)
}
