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
    <!IDENTITY_HASH_CODE_ON_VALUE_TYPE!>t.identityHashCode()<!>
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>t<!>)
}

fun willBecomeValue(wrapper: Wrapper) {
    WeakReference(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>wrapper<!>)
}
