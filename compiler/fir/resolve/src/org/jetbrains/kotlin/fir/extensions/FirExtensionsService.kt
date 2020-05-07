/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.getSymbolByTypeRef
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.utils.*
import org.jetbrains.kotlin.name.FqName
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class FirRegisteredExtension<P : FirExtension>(
    val extensionsWithAllMode: List<P>,
    val extensionsWithAnnotatedMode: Multimap<AnnotationFqn, P>,
    val extensionsWithAllInAnnotatedMode: Multimap<AnnotationFqn, P>,
    val extensionsByPluginKey: Multimap<FirPluginKey, P>
) {
    fun isEmpty(): Boolean {
        return extensionsWithAllMode.isEmpty() && extensionsWithAnnotatedMode.isEmpty() && extensionsWithAllInAnnotatedMode.isEmpty()
    }
}

class FirExtensionsService(
    val session: FirSession
) : ComponentArrayOwner<FirExtension, FirRegisteredExtension<*>>(), FirSessionComponent {
    companion object : TypeRegistry<FirExtension, FirRegisteredExtension<*>>() {
        inline fun <reified P : FirExtension, V : FirRegisteredExtension<P>> registeredExtensions(): ReadOnlyProperty<FirExtensionsService, ExtensionsAccessor<P>> {
            val accessor = generateAccessor<V, P>(P::class)
            return object : ReadOnlyProperty<FirExtensionsService, ExtensionsAccessor<P>> {
                override fun getValue(thisRef: FirExtensionsService, property: KProperty<*>): ExtensionsAccessor<P> {
                    return ExtensionsAccessor(thisRef.session, accessor.getValue(thisRef, property))
                }
            }
        }

        private fun <K, V> createMultimap(): Multimap<K, V> = LinkedHashMultimap.create()
    }

    override val typeRegistry: TypeRegistry<FirExtension, FirRegisteredExtension<*>>
        get() = Companion

    fun <P : FirExtension> registerExtensions(extensionClass: KClass<P>, extensionFactories: List<FirExtension.Factory<P>>) {
        registeredExtensionsSize += extensionFactories.size
        val extensions = extensionFactories.map { it.create(session) }

        val extensionsWithAllMode = mutableListOf<P>()
        val extensionsWithAnnotatedMode = createMultimap<AnnotationFqn, P>()
        val extensionsWithAllInAnnotatedMode = createMultimap<AnnotationFqn, P>()
        val extensionsByPlugin = createMultimap<FirPluginKey, P>()
        for (extension in extensions) {
            _metaAnnotations += extension.metaAnnotations.keys
            for (metaAnnotation in extension.metaAnnotations.keys) {
                extensionsWithMetaAnnotations.put(metaAnnotation, extension)
            }
            _annotations += extension.directlyApplicableAnnotations
            _annotations += extension.childrenApplicableAnnotations
            when (extension.mode) {
                FirExtension.Mode.ANNOTATED_ELEMENT -> {
                    for (annotation in extension.directlyApplicableAnnotations) {
                        extensionsWithAnnotatedMode.put(annotation, extension)
                    }
                    for (annotation in extension.childrenApplicableAnnotations) {
                        extensionsWithAllInAnnotatedMode.put(annotation, extension)
                    }
                }
                FirExtension.Mode.ALL -> extensionsWithAllMode += extension
            }
            extensionsByPlugin.put(extension.key, extension)
        }
        registerComponent(
            extensionClass,
            FirRegisteredExtension(
                extensionsWithAllMode,
                extensionsWithAnnotatedMode,
                extensionsWithAllInAnnotatedMode,
                extensionsByPlugin
            )
        )
    }

    fun registerUserDefinedAnnotation(metaAnnotation: AnnotationFqn, annotations: Collection<FirRegularClass>) {
        for (annotation in annotations) {
            require(annotation.classKind == ClassKind.ANNOTATION_CLASS)
            val fqName = annotation.symbol.classId.asSingleFqName()
            _annotations += fqName
            val extensions = extensionsWithMetaAnnotations[metaAnnotation]
            if (extensions.isEmpty()) return
            for (extension in extensions) {
                val registeredExtensions = this[extension.extensionType]

                val metaAnnotationMode = extension.metaAnnotations.getValue(metaAnnotation)

                if (metaAnnotationMode.directed) {
                    @Suppress("UNCHECKED_CAST")
                    (registeredExtensions.extensionsWithAnnotatedMode as Multimap<AnnotationFqn, FirExtension>).put(fqName, extension)
                }

                if (metaAnnotationMode.children) {
                    @Suppress("UNCHECKED_CAST")
                    (registeredExtensions.extensionsWithAllInAnnotatedMode as Multimap<AnnotationFqn, FirExtension>).put(fqName, extension)
                }
            }
        }
    }

    val annotations: Set<AnnotationFqn>
        get() = _annotations
    private val _annotations: MutableSet<AnnotationFqn> = mutableSetOf()

    val metaAnnotations: Set<AnnotationFqn>
        get() = _metaAnnotations
    private val _metaAnnotations: MutableSet<AnnotationFqn> = mutableSetOf()

    private val extensionsWithMetaAnnotations: Multimap<AnnotationFqn, FirExtension> = createMultimap()

    var registeredExtensionsSize: Int = 0
        private set

    class ExtensionsAccessor<P : FirExtension>(
        private val session: FirSession,
        private val extensions: FirRegisteredExtension<P>
    ) {
        fun forDeclaration(declaration: FirAnnotationContainer, owners: Collection<FirAnnotationContainer>): Collection<P> {
            if (extensions.isEmpty()) return emptySet()
            if (
                (declaration.annotations.isEmpty() || extensions.extensionsWithAnnotatedMode.isEmpty) &&
                extensions.extensionsWithAllInAnnotatedMode.isEmpty
            ) {
                return extensions.extensionsWithAllMode
            }

            return LinkedHashSet(extensions.extensionsWithAllMode).apply {
                collectExtensions(this, declaration, extensions.extensionsWithAnnotatedMode)
                collectExtensions(this, declaration, extensions.extensionsWithAllInAnnotatedMode)
                for (owner in owners) {
                    collectExtensions(this, owner, extensions.extensionsWithAllInAnnotatedMode)
                }
            }
        }

        fun forGeneratedDeclaration(origin: FirDeclarationOrigin.Plugin): Collection<P> {
            return extensions.extensionsByPluginKey.get(origin.key)
        }

        private fun collectExtensions(result: MutableSet<P>, declaration: FirAnnotationContainer, extensions: Multimap<AnnotationFqn, P>) {
            for (annotation in declaration.annotations) {
                val fqName = annotation.fqName(session) ?: continue
                result += extensions[fqName]
            }
        }
    }
}

val FirSession.extensionsService: FirExtensionsService by FirSession.sessionComponentAccessor()

fun FirAnnotationCall.fqName(session: FirSession): FqName? {
    val symbol = session.firSymbolProvider.getSymbolByTypeRef<FirRegularClassSymbol>(annotationTypeRef) ?: return null
    return symbol.classId.asSingleFqName()
}

val FirExtensionsService.hasExtensions: Boolean
    get() = registeredExtensionsSize > 0