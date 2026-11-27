// ISSUE: KT-89982
// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +FullValueClasses
@file:OptIn(ExperimentalNativeApi::class, ExperimentalValueClassesApi::class)

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference
import kotlin.native.ref.createCleaner

inline value class Inline(val a: Int)

value class Full(val a: Int, val b: Int)

value class Single(val a: Int)

class Identity

@WillBecomeValue
data class WBV(val a: Int)

fun test(inline: Inline, full: Full, single: Single, int: Int, identity: Identity, wbv: WBV) {
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>inline<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>full<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>single<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>int<!>)
    WeakReference(identity)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>wbv<!>)
    // A cleaner runs when the cleaner itself is collected, not its resource, so the resource may be a value.
    createCleaner(inline) {}
    createCleaner(full) {}
    createCleaner(identity) {}
    createCleaner(wbv) {}
    createCleaner(cleanupAction = {}, resource = full)
    createCleaner(cleanupAction = {}, resource = identity)
}
