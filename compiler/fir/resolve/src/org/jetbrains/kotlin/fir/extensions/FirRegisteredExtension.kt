/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.utils.ComponentArrayOwner
import org.jetbrains.kotlin.fir.utils.ComponentTypeRegistry
import org.jetbrains.kotlin.name.Name
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class FirExtensionPoint(val session: FirSession) {
    abstract val name: FirExtensionPointName

    fun interface Factory<P : FirExtensionPoint> {
        fun create(session: FirSession): P
    }
}

data class FirExtensionPointName(val name: Name) {
    constructor(name: String) : this(Name.identifier(name))
}

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class FirRegisteredExtension<P : FirExtensionPoint>(val extensions: List<P>)

class FirExtensionPointService(
    private val session: FirSession
) : ComponentArrayOwner<FirExtensionPoint, FirRegisteredExtension<*>>(), FirSessionComponent {
    companion object : ComponentTypeRegistry<FirExtensionPoint, FirRegisteredExtension<*>>() {
        inline fun <reified K : FirExtensionPoint, V : FirRegisteredExtension<K>> registeredExtensions(): ReadOnlyProperty<FirExtensionPointService, List<K>> {
            val accessor = generateAccessor<V, K>(K::class)
            return object : ReadOnlyProperty<FirExtensionPointService, List<K>> {
                override fun getValue(thisRef: FirExtensionPointService, property: KProperty<*>): List<K> {
                    return accessor.getValue(thisRef, property).extensions
                }
            }
        }
    }

    override val typeRegistry: ComponentTypeRegistry<FirExtensionPoint, FirRegisteredExtension<*>>
        get() = Companion

    fun <P : FirExtensionPoint> registerExtensions(extensionClass: KClass<P>, extensions: List<FirExtensionPoint.Factory<P>>) {
        registerComponent(extensionClass, FirRegisteredExtension(extensions.map { it.create(session) }))
    }
}

val FirSession.extensionPointService: FirExtensionPointService by FirSession.sessionComponentAccessor()
