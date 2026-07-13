/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.javaInterop

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType

/**
 * Converts the given [PsiType] to a [KaType] in the context of the [useSitePosition].
 *
 * [useSitePosition] clarifies how to resolve some parts of the [PsiType]. For instance, it can be used to collect type parameters and
 * apply them during the conversion.
 *
 * @receiver The [PsiType] to be converted.
 *
 * @return The converted [KaType], or `null` if conversion is not possible. For example, [PsiType] might not be resolvable.
 */
@KaExperimentalApi
context(session: KaSession)
public fun PsiType.asKaType(useSitePosition: PsiElement): KaType? {
    @OptIn(KaImplementationDetail::class)
    return internals.javaInteroperabilityComponent.asKaType(this, useSitePosition)
}

/**
 * Convert the given [KaType] to a JVM type descriptor with the [KaTypeMappingMode.DEFAULT][org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode.DEFAULT].
 * To learn more about JVM descriptors, check out the
 * [JVM specification](https://docs.oracle.com/javase/specs/jvms/se24/html/jvms-4.html#jvms-4.3).
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaType.mapToJvmTypeDescriptor(): String {
    @OptIn(KaImplementationDetail::class)
    return internals.javaInteroperabilityComponent.mapToJvmTypeDescriptor(this)
}

/**
 * Whether the given [KaType] is backed by a single JVM primitive type.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaType.isPrimitiveBacked: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.javaInteroperabilityComponent.isPrimitiveBacked(this)
    }

/**
 * A [KaNamedClassSymbol] for the given [PsiClass], or `null` for anonymous classes, local classes, type parameters (which are also
 * [PsiClass]es), and Kotlin light classes.
 */
context(session: KaSession)
public val PsiClass.namedClassSymbol: KaNamedClassSymbol?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.javaInteroperabilityComponent.namedClassSymbol(this)
    }

/**
 * A [KaCallableSymbol] for the given [PsiMember] method or field, or `null` for local declarations and Kotlin light classes.
 */
context(session: KaSession)
public val PsiMember.callableSymbol: KaCallableSymbol?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.javaInteroperabilityComponent.callableSymbol(this)
    }

/**
 * The containing JVM class name for the given [KaCallableSymbol].
 *
 * The property works for both source and library declarations.
 * The JVM class name is a fully qualified name separated by dots, such as `foo.bar.Baz.Companion`.
 *
 * Applicable only to JVM modules, and common modules with JVM targets.
 * [containingJvmClassName] is always `null` all other kinds of modules.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaCallableSymbol.containingJvmClassName: String?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.javaInteroperabilityComponent.containingJvmClassName(this)
    }
