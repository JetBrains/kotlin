/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.backend.konan.CachePolicy
import org.jetbrains.kotlin.config.keys.generator.model.DeprecatedKey
import org.jetbrains.kotlin.config.keys.generator.model.Key
import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType

/**
 * The keys of the Kotlin/Native compiler. Each key declares its [CachePolicy],
 * and the generated object gets the `cachePolicies` map.
 */
abstract class NativeKeysContainer(packageName: String, className: String) : KeysContainer(packageName, className) {
    @PublishedApi
    internal val cachePolicies = mutableMapOf<String, CachePolicy>()

    inline fun <reified T : Any> key(
        comment: String? = null,
        cachePolicy: CachePolicy,
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, Key>> = PropertyDelegateProvider { thisRef, property ->
        cachePolicies[property.name] = cachePolicy
        key<T>(comment).provideDelegate(thisRef, property)
    }

    override val additionalImportableTypes: List<KType>
        get() = listOf(CachePolicy::class.starProjectedType)

    override fun SmartPrinter.generateAdditionalMembers() {
        val regularKeys = keys.filter { it !is DeprecatedKey }
        val withoutPolicy = regularKeys.filter { it.name !in cachePolicies }
        require(withoutPolicy.isEmpty()) { "Specify the cache policy for ${withoutPolicy.map { it.name }} in $className" }

        println("// How the options interact with the compiler caches.")
        println("@JvmField")
        println("val cachePolicies: Map<CompilerConfigurationKey<*>, CachePolicy> = mapOf(")
        withIndent {
            for (key in regularKeys) {
                println("${key.name} to CachePolicy.${cachePolicies.getValue(key.name).name},")
            }
        }
        println(")")
    }
}
