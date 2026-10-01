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
    WeakReference(full)
    WeakReference(single)
    WeakReference(int)
    WeakReference(identity)
    createCleaner(full) {}
    createCleaner(identity) {}
    createCleaner(cleanupAction = {}, resource = full)
    createCleaner(cleanupAction = {}, resource = identity)
}
