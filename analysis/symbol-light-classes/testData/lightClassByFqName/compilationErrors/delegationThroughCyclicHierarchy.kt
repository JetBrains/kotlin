// main.impl.DynamicFeatureExtension
// IGNORE_FIR
// KT-64637: delegation to an interface whose hierarchy is cyclic because a dependency is missing.
// `main.api.DynamicFeatureExtension` does not exist, so `DynamicFeatureExtension` in
// `InternalDynamicFeatureExtension` resolves to `main.impl.DynamicFeatureExtension`, which closes a cycle.

// MODULE: lib
// FILE: pkg/api/Action.kt
package pkg.api

interface Action<T> {
    fun execute(t: T)
}

// FILE: my/Sandbox.kt
package my

interface Sandbox

// MODULE: main(lib)
// FILE: main/test/InternalTestExtension.kt
package main.test

interface InternalTestExtension

// FILE: main/impl/InternalDynamicFeatureExtension.kt
package main.impl

import pkg.api.Action
import my.Sandbox
import main.api.DynamicFeatureExtension
import main.test.InternalTestExtension

interface InternalDynamicFeatureExtension : DynamicFeatureExtension, InternalTestExtension {
    fun sandbox(action: Action<Sandbox>)
}

// FILE: main/impl/DynamicFeatureExtensionImpl.kt
package main.impl

abstract class DynamicFeatureExtensionImpl : InternalDynamicFeatureExtension

// FILE: main/impl/DynamicFeatureExtension.kt
package main.impl

abstract class DynamicFeatureExtension(
    private val publicExtensionImpl: DynamicFeatureExtensionImpl
) : InternalDynamicFeatureExtension by publicExtensionImpl
