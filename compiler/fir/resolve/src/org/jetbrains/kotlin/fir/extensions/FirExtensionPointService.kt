/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.getSymbolByTypeRef
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.utils.ComponentArrayOwner
import org.jetbrains.kotlin.fir.utils.ComponentTypeRegistry
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class FirRegisteredExtension<P : FirExtensionPoint>(val extensions: Multimap<AnnotationFqn, P>)

class FirExtensionPointService(
    val session: FirSession
) : ComponentArrayOwner<FirExtensionPoint, FirRegisteredExtension<*>>(), FirSessionComponent {
    companion object : ComponentTypeRegistry<FirExtensionPoint, FirRegisteredExtension<*>>() {
        inline fun <reified P : FirExtensionPoint, V : FirRegisteredExtension<P>> registeredExtensions(): ReadOnlyProperty<FirExtensionPointService, ExtensionsAccessor<P>> {
            val accessor = generateAccessor<V, P>(P::class)
            return object : ReadOnlyProperty<FirExtensionPointService, ExtensionsAccessor<P>> {
                override fun getValue(thisRef: FirExtensionPointService, property: KProperty<*>): ExtensionsAccessor<P> {
                    return ExtensionsAccessor(thisRef.session, accessor.getValue(thisRef, property).extensions)
                }
            }
        }

        private fun <K, V> createMultimap(): Multimap<K, V> = LinkedHashMultimap.create()
    }

    override val typeRegistry: ComponentTypeRegistry<FirExtensionPoint, FirRegisteredExtension<*>>
        get() = Companion

    fun <P : FirExtensionPoint> registerExtensions(extensionClass: KClass<P>, extensionFactories: List<FirExtensionPoint.Factory<P>>) {
        val extensions = extensionFactories.map { it.create(session) }
        val map = createMultimap<AnnotationFqn, P>()
        for (extension in extensions) {
            _metaAnnotations += extension.metaAnnotations
            for (annotation in extension.annotations) {
                _annotations += annotation
                map.put(annotation, extension)
            }
        }
        registerComponent(extensionClass, FirRegisteredExtension(map))
    }

    val annotations: Set<AnnotationFqn>
        get() = _annotations
    private val _annotations: MutableSet<AnnotationFqn> = mutableSetOf()

    val metaAnnotations: Set<AnnotationFqn>
        get() = _metaAnnotations
    private val _metaAnnotations: MutableSet<AnnotationFqn> = mutableSetOf()

    class ExtensionsAccessor<P : FirExtensionPoint>(
        private val session: FirSession,
        private val extensions: Multimap<AnnotationFqn, P>
    ) {
        fun forDeclaration(declaration: FirAnnotationContainer): Collection<P> {
            if (declaration.annotations.isEmpty()) return emptySet()
            val result = mutableSetOf<P>()
            for (annotation in declaration.annotations) {
                val symbol = session.firSymbolProvider.getSymbolByTypeRef<FirRegularClassSymbol>(annotation.annotationTypeRef) ?: continue
                val fqName = symbol.classId.asSingleFqName()
                result += extensions[fqName]
            }
            return result
        }
    }
}

val FirSession.extensionPointService: FirExtensionPointService by FirSession.sessionComponentAccessor()