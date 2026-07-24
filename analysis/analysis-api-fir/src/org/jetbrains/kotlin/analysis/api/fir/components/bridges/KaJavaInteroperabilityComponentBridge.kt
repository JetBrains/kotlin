/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.components.KaJavaInteroperabilityComponent
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.fir.components.createFirJvmTypeMapper
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsJavaInteroperabilityComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.Name
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.kotlin.analysis.api.javaInterop.asKaType as asKaTypeEndpoint
import org.jetbrains.kotlin.analysis.api.javaInterop.callableSymbol as callableSymbolEndpoint
import org.jetbrains.kotlin.analysis.api.javaInterop.containingJvmClassName as containingJvmClassNameEndpoint
import org.jetbrains.kotlin.analysis.api.javaInterop.isPrimitiveBacked as isPrimitiveBackedEndpoint
import org.jetbrains.kotlin.analysis.api.javaInterop.mapToJvmTypeDescriptor as mapToJvmTypeDescriptorEndpoint
import org.jetbrains.kotlin.analysis.api.javaInterop.namedClassSymbol as namedClassSymbolEndpoint

internal class KaJavaInteroperabilityComponentBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaJavaInteroperabilityComponent, KaFirSessionComponent {
    private val proxy: KaInternalsJavaInteroperabilityComponent
        get() = analysisSession.javaInteroperabilityComponent

    private val jvmTypeMapper: FirJvmTypeMapper by lazy { createFirJvmTypeMapper(analysisSession) }

    // Migrated callables — routed through the public endpoints.

    override fun PsiType.asKaType(useSitePosition: PsiElement): KaType? =
        context(analysisSession) { asKaTypeEndpoint(useSitePosition) }

    override fun KaType.mapToJvmTypeDescriptor(): String =
        context(analysisSession) { mapToJvmTypeDescriptorEndpoint() }

    override val KaType.isPrimitiveBacked: Boolean
        get() = context(analysisSession) { isPrimitiveBackedEndpoint }

    override val PsiClass.namedClassSymbol: KaNamedClassSymbol?
        get() = context(analysisSession) { namedClassSymbolEndpoint }

    override val PsiMember.callableSymbol: KaCallableSymbol?
        get() = context(analysisSession) { callableSymbolEndpoint }

    override val KaCallableSymbol.containingJvmClassName: String?
        get() = context(analysisSession) { containingJvmClassNameEndpoint }

    // Ignored callables (no public endpoint yet) — routed directly through the proxy.

    override fun KaType.asPsiType(
        useSitePosition: PsiElement,
        allowErrorTypes: Boolean,
        mode: KaTypeMappingMode,
        isAnnotationMethod: Boolean,
        suppressWildcards: Boolean?,
        preserveAnnotations: Boolean,
        allowNonJvmPlatforms: Boolean,
    ): PsiType? = proxy.asPsiType(
        this,
        useSitePosition,
        allowErrorTypes,
        mode,
        isAnnotationMethod,
        suppressWildcards,
        preserveAnnotations,
        allowNonJvmPlatforms,
    )

    @Deprecated("Use 'mapToJvmTypeDescriptor' instead.", level = DeprecationLevel.HIDDEN)
    override fun KaType.mapToJvmType(mode: TypeMappingMode): Type = withValidityAssertion {
        jvmTypeMapper.mapType(coneType, mode, sw = null, unresolvedQualifierRemapper = null)
    }

    override val KaPropertySymbol.javaGetterName: Name
        get() = proxy.javaGetterName(this)

    override val KaPropertySymbol.javaSetterName: Name?
        get() = proxy.javaSetterName(this)
}
