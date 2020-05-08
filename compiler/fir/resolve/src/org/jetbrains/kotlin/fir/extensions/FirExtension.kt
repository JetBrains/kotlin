/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass

typealias AnnotationFqn = FqName

abstract class FirExtension(val session: FirSession) {
    abstract val name: FirExtensionPointName

    abstract val directlyApplicableAnnotations: Set<AnnotationFqn>
    abstract val childrenApplicableAnnotations: Set<AnnotationFqn>
    abstract val metaAnnotations: Map<AnnotationFqn, MetaAnnotationMode>

    abstract val mode: Mode
    abstract val key: FirPluginKey

    internal abstract val extensionType: KClass<out FirExtension>

    fun interface Factory<P : FirExtension> {
        fun create(session: FirSession): P
    }

    enum class Mode {
        ANNOTATED_ELEMENT,
        ALL
    }

    enum class MetaAnnotationMode(val directed: Boolean, val children: Boolean) {
        ANNOTATED_DECLARATION(directed = true, children = false),
        CHILDREN_DECLARATION(directed = false, children = true),
        ANNOTATED_AND_CHILDREN(directed = true, children = true)
    }
}

data class FirExtensionPointName(val name: Name) {
    constructor(name: String) : this(Name.identifier(name))
}
