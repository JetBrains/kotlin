// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
@file:OptIn(ExperimentalNativeApi::class)

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference
import kotlin.native.ref.createCleaner

value class Full(val a: Int, val b: Int)

value class Single(val a: Int)

class Identity

fun test(full: Full, single: Single, int: Int, identity: Identity) {
    WeakReference(<!IDENTITY_SENSITIVE_OPERATION_ON_VALUE_TYPE!>full<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATION_ON_VALUE_TYPE!>single<!>)
    WeakReference(<!IDENTITY_SENSITIVE_OPERATION_ON_VALUE_TYPE!>int<!>)
    WeakReference(identity)
    createCleaner(<!IDENTITY_SENSITIVE_OPERATION_ON_VALUE_TYPE!>full<!>) {}
    createCleaner(identity) {}
    createCleaner(cleanupAction = {}, resource = <!IDENTITY_SENSITIVE_OPERATION_ON_VALUE_TYPE!>full<!>)
    createCleaner(cleanupAction = {}, resource = identity)
}
