/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.components.KtSymbolContainingDeclarationProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.getContainingKtModule
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.jvmClassNameIfDeserialized
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalDeclaration
import org.jetbrains.kotlin.analysis.project.structure.DanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.project.structure.KtDanglingFileModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.diagnostics.ConeDestructuringDeclarationsOnTopLevel
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorPropertySymbol
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KtFirSymbolContainingDeclarationProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtSymbolContainingDeclarationProvider(), KtFirAnalysisSessionComponent {
    override fun getContainingDeclaration(symbol: KtSymbol): KtDeclarationSymbol? {
        if (!hasParentSymbol(symbol)) {
            return null
        }

        val firSymbol = symbol.firSymbol
        val symbolFirSession = firSymbol.llFirSession
        val symbolModule = symbolFirSession.ktModule

        if (firSymbol is FirErrorPropertySymbol && firSymbol.diagnostic is ConeDestructuringDeclarationsOnTopLevel) {
            return null
        }

        getContainingDeclarationForDependentDeclaration(symbol)?.let { return it }

        if (symbolModule is KtDanglingFileModule && symbolModule.resolutionMode == DanglingFileResolutionMode.IGNORE_SELF) {
            if (hasParentPsi(symbol)) {
                // getSymbol(ClassId) returns a symbol from the original file, so here we avoid using it
                return getContainingDeclarationByPsi(symbol)
            }
        }

        when (symbol) {
            is KtLocalVariableSymbol,
            is KtAnonymousFunctionSymbol,
            is KtAnonymousObjectSymbol,
            is KtDestructuringDeclarationSymbol -> {
                return getContainingDeclarationByPsi(symbol)
            }

            is KtClassInitializerSymbol -> {
                val outerFirClassifier = firSymbol.getContainingClassSymbol(symbolFirSession)
                if (outerFirClassifier != null) {
                    return firSymbolBuilder.buildSymbol(outerFirClassifier) as? KtDeclarationSymbol
                }
            }

            is KtValueParameterSymbol -> {
                return firSymbolBuilder.callableBuilder.buildCallableSymbol(symbol.firSymbol.fir.containingFunctionSymbol)
            }

            is KtCallableSymbol -> {
                val outerFirClassifier = firSymbol.getContainingClassSymbol(symbolFirSession)
                if (outerFirClassifier != null) {
                    return firSymbolBuilder.buildSymbol(outerFirClassifier) as? KtDeclarationSymbol
                }

                if (firSymbol.origin == FirDeclarationOrigin.DynamicScope) {
                    // A callable declaration from dynamic scope has no containing declaration as it comes from a dynamic type
                    // which is not based on a specific classifier
                    return null
                }
            }

            is KtClassLikeSymbol -> {
                val outerClassId = symbol.classIdIfNonLocal?.outerClassId
                if (outerClassId != null) { // Won't work for local and top-level classes, or classes inside a script
                    val outerFirClassifier = symbolFirSession.firProvider.getFirClassifierByFqName(outerClassId) ?: return null
                    return firSymbolBuilder.buildSymbol(outerFirClassifier) as? KtDeclarationSymbol
                }
            }
        }

        return getContainingDeclarationByPsi(symbol)
    }

    private fun hasParentSymbol(symbol: KtSymbol): Boolean {
        when (symbol) {
            is KtReceiverParameterSymbol -> {
                // KT-55124
                return true
            }

            !is KtDeclarationSymbol -> {
                // File, package, etc.
                return false
            }

            is KtSamConstructorSymbol -> {
                // SAM constructors are always top-level
                return false
            }

            is KtScriptSymbol -> {
                // Scripts are always top-level
                return false
            }

            is KtSymbolWithKind -> {
                if (symbol.symbolKind == KtSymbolKind.TOP_LEVEL) {
                    val containingFile = (symbol.firSymbol.fir as? FirElementWithResolveState)?.getContainingFile()
                    if (containingFile == null || containingFile.declarations.firstOrNull() !is FirScript) {
                        // Should be replaced with proper check after KT-61451 and KT-61887
                        return false
                    }
                }

                return true
            }

            else -> {
                return true
            }
        }
    }

    fun getContainingDeclarationByPsi(symbol: KtSymbol): KtDeclarationSymbol? {
        val containingDeclaration = getContainingPsi(symbol) ?: return null
        return with(analysisSession) { containingDeclaration.getSymbol() }
    }

    private fun getContainingDeclarationForDependentDeclaration(symbol: KtSymbol): KtDeclarationSymbol? {
        return when (symbol) {
            is KtReceiverParameterSymbol -> symbol.owningCallableSymbol
            is KtBackingFieldSymbol -> symbol.owningProperty
            is KtPropertyAccessorSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.propertySymbol) as KtDeclarationSymbol
            is KtTypeParameterSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.containingDeclarationSymbol) as? KtDeclarationSymbol
            is KtValueParameterSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.containingFunctionSymbol) as? KtDeclarationSymbol
            else -> null
        }
    }

    override fun getContainingFileSymbol(symbol: KtSymbol): KtFileSymbol? {
        if (symbol is KtFileSymbol) return null
        val firSymbol = when (symbol) {
            is KtFirReceiverParameterSymbol -> {
                // symbol from receiver parameter
                symbol.firSymbol
            }
            else -> {
                // general FIR-based symbol
                symbol.firSymbol
            }
        }
        val firFileSymbol = firSymbol.fir.getContainingFile()?.symbol ?: return null
        return firSymbolBuilder.buildFileSymbol(firFileSymbol)
    }

    override fun getContainingJvmClassName(symbol: KtCallableSymbol): String? {
        val platform = getContainingModule(symbol).platform
        if (!platform.has<JvmPlatform>()) return null

        val containingSymbolOrSelf = when (symbol) {
            is KtValueParameterSymbol -> {
                getContainingDeclaration(symbol) as? KtFunctionLikeSymbol ?: symbol
            }
            is KtPropertyAccessorSymbol -> {
                getContainingDeclaration(symbol) as? KtPropertySymbol ?: symbol
            }
            is KtBackingFieldSymbol -> symbol.owningProperty
            else -> symbol
        }
        val firSymbol = containingSymbolOrSelf.firSymbol

        firSymbol.jvmClassNameIfDeserialized()?.let {
            return it.fqNameForClassNameWithoutDollars.asString()
        }

        return if (containingSymbolOrSelf.symbolKind == KtSymbolKind.TOP_LEVEL) {
            (firSymbol.fir.getContainingFile()?.psi as? KtFile)
                ?.takeUnless { it.isScript() }
                ?.javaFileFacadeFqName?.asString()
        } else {
            val classId = (containingSymbolOrSelf as? KtConstructorSymbol)?.containingClassIdIfNonLocal
                ?: containingSymbolOrSelf.callableIdIfNonLocal?.classId
            classId?.takeUnless { it.shortClassName.isSpecial }
                ?.asFqNameString()
        }
    }

    override fun getContainingModule(symbol: KtSymbol): KtModule {
        return symbol.getContainingKtModule(analysisSession.firResolveSession)
    }

    private fun getContainingPsi(symbol: KtSymbol): KtDeclaration? {
        val source = symbol.firSymbol.source
            ?: errorWithAttachment("PSI should present for declaration built by Kotlin code") {
                withSymbolAttachment("symbolForContainingPsi", symbol, analysisSession)
            }

        getContainingPsiForFakeSource(source)?.let { return it }

        val psi = source.psi
            ?: errorWithAttachment("PSI not found for source kind '${source.kind}'") {
                withSymbolAttachment("symbolForContainingPsi", symbol, analysisSession)
            }

        if (source.kind != KtRealSourceElementKind) {
            errorWithAttachment("Cannot compute containing PSI for unknown source kind '${source.kind}' (${psi::class.simpleName})") {
                withSymbolAttachment("symbolForContainingPsi", symbol, analysisSession)
            }
        }

        if (isSyntheticSymbolWithParentSource(symbol)) {
            return psi as KtDeclaration
        }

        if (isOrdinarySymbolWithSource(symbol)) {
            val result = psi.getContainingPsiDeclaration()

            if (result == null) {
                val containingFile = psi.containingFile
                if (containingFile is KtCodeFragment) {
                    // All content inside a code fragment is implicitly local, but there is no non-local parent
                    return null
                }

                errorWithAttachment("Containing declaration should present for nested declaration ${psi::class}") {
                    withSymbolAttachment("symbolForContainingPsi", symbol, analysisSession)
                }
            }

            return result
        }

        errorWithAttachment("Unsupported declaration origin ${symbol.origin} ${psi::class}") {
            withSymbolAttachment("symbolForContainingPsi", symbol, analysisSession)
        }
    }

    private fun hasParentPsi(symbol: KtSymbol): Boolean {
        val source = symbol.firSymbol.source?.takeIf { it.psi is KtElement } ?: return false

        return getContainingPsiForFakeSource(source) != null
                || isSyntheticSymbolWithParentSource(symbol)
                || isOrdinarySymbolWithSource(symbol)
    }

    private fun isSyntheticSymbolWithParentSource(symbol: KtSymbol): Boolean {
        return when (symbol.origin) {
            KtSymbolOrigin.SOURCE_MEMBER_GENERATED -> true
            else -> false
        }
    }

    private fun isOrdinarySymbolWithSource(symbol: KtSymbol): Boolean {
        return symbol.origin == KtSymbolOrigin.SOURCE
                || symbol.firSymbol.fir.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty
    }

    private fun getContainingPsiForFakeSource(source: KtSourceElement): KtDeclaration? {
        return when (source.kind) {
            KtFakeSourceElementKind.ImplicitConstructor -> source.psi as KtDeclaration
            KtFakeSourceElementKind.PropertyFromParameter -> source.psi?.parentOfType<KtPrimaryConstructor>()!!
            KtFakeSourceElementKind.EnumInitializer -> source.psi as KtEnumEntry
            KtFakeSourceElementKind.EnumGeneratedDeclaration -> source.psi as KtDeclaration
            KtFakeSourceElementKind.ScriptParameter -> source.psi as KtScript
            else -> null
        }
    }

    private fun PsiElement.getContainingPsiDeclaration(): KtDeclaration? {
        for (parent in parents) {
            if (parent is KtDeclaration && parent !is KtDestructuringDeclaration) {
                return parent.originalDeclaration ?: parent
            }
        }

        return null
    }
}
