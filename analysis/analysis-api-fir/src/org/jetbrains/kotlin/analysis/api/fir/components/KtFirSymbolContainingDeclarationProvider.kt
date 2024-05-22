/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.components.KaSymbolContainingDeclarationProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.getContainingKtModule
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.isForeignValue
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
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFirSymbolContainingDeclarationProvider(
    override val analysisSession: KaFirSession,
    override val token: KaLifetimeToken,
) : KaSymbolContainingDeclarationProvider(), KaFirSessionComponent {
    override fun getContainingDeclaration(symbol: KaSymbol): KaDeclarationSymbol? {
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
            is KaLocalVariableSymbol,
            is KaAnonymousFunctionSymbol,
            is KaAnonymousObjectSymbol,
            is KaDestructuringDeclarationSymbol -> {
                return getContainingDeclarationByPsi(symbol)
            }

            is KaClassInitializerSymbol -> {
                val outerFirClassifier = firSymbol.getContainingClassSymbol(symbolFirSession)
                if (outerFirClassifier != null) {
                    return firSymbolBuilder.buildSymbol(outerFirClassifier) as? KaDeclarationSymbol
                }
            }

            is KaValueParameterSymbol -> {
                return firSymbolBuilder.callableBuilder.buildCallableSymbol(symbol.firSymbol.fir.containingFunctionSymbol)
            }

            is KaCallableSymbol -> {
                val outerFirClassifier = firSymbol.getContainingClassSymbol(symbolFirSession)
                if (outerFirClassifier != null) {
                    return firSymbolBuilder.buildSymbol(outerFirClassifier) as? KaDeclarationSymbol
                }

                if (firSymbol.origin == FirDeclarationOrigin.DynamicScope) {
                    // A callable declaration from dynamic scope has no containing declaration as it comes from a dynamic type
                    // which is not based on a specific classifier
                    return null
                }
            }

            is KaClassLikeSymbol -> {
                val outerClassId = symbol.classId?.outerClassId
                if (outerClassId != null) { // Won't work for local and top-level classes, or classes inside a script
                    val outerFirClassifier = symbolFirSession.firProvider.getFirClassifierByFqName(outerClassId) ?: return null
                    return firSymbolBuilder.buildSymbol(outerFirClassifier) as? KaDeclarationSymbol
                }
            }
        }

        return getContainingDeclarationByPsi(symbol)
    }

    private fun hasParentSymbol(symbol: KaSymbol): Boolean {
        when (symbol) {
            is KaReceiverParameterSymbol -> {
                // KT-55124
                return true
            }

            !is KaDeclarationSymbol -> {
                // File, package, etc.
                return false
            }

            is KaSamConstructorSymbol -> {
                // SAM constructors are always top-level
                return false
            }

            is KaScriptSymbol -> {
                // Scripts are always top-level
                return false
            }

            else -> {}
        }

        if (symbol is KaSymbolWithKind && symbol.symbolKind == KaSymbolKind.TOP_LEVEL) {
            val containingFile = (symbol.firSymbol.fir as? FirElementWithResolveState)?.getContainingFile()
            if (containingFile == null || containingFile.declarations.firstOrNull() !is FirScript) {
                // Should be replaced with proper check after KT-61451 and KT-61887
                return false
            }
        }

        val firSymbol = symbol.firSymbol
        if (firSymbol is FirPropertySymbol && firSymbol.isForeignValue) {
            return false
        }

        return true
    }

    fun getContainingDeclarationByPsi(symbol: KaSymbol): KaDeclarationSymbol? {
        val containingDeclaration = getContainingPsi(symbol) ?: return null
        return with(analysisSession) { containingDeclaration.getSymbol() }
    }

    private fun getContainingDeclarationForDependentDeclaration(symbol: KaSymbol): KaDeclarationSymbol? {
        return when (symbol) {
            is KaReceiverParameterSymbol -> symbol.owningCallableSymbol
            is KaBackingFieldSymbol -> symbol.owningProperty
            is KaPropertyAccessorSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.propertySymbol) as KaDeclarationSymbol
            is KaTypeParameterSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.containingDeclarationSymbol) as? KaDeclarationSymbol
            is KaValueParameterSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.containingFunctionSymbol) as? KaDeclarationSymbol
            else -> null
        }
    }

    override fun getContainingFileSymbol(symbol: KaSymbol): KaFileSymbol? {
        if (symbol is KaFileSymbol) return null
        val firSymbol = when (symbol) {
            is KaFirReceiverParameterSymbol -> {
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

    override fun getContainingJvmClassName(symbol: KaCallableSymbol): String? {
        val platform = getContainingModule(symbol).platform
        if (!platform.has<JvmPlatform>()) return null

        val containingSymbolOrSelf = when (symbol) {
            is KaValueParameterSymbol -> {
                getContainingDeclaration(symbol) as? KaFunctionLikeSymbol ?: symbol
            }
            is KaPropertyAccessorSymbol -> {
                getContainingDeclaration(symbol) as? KaPropertySymbol ?: symbol
            }
            is KaBackingFieldSymbol -> symbol.owningProperty
            else -> symbol
        }
        val firSymbol = containingSymbolOrSelf.firSymbol

        firSymbol.jvmClassNameIfDeserialized()?.let {
            return it.fqNameForClassNameWithoutDollars.asString()
        }

        return if (containingSymbolOrSelf.symbolKind == KaSymbolKind.TOP_LEVEL) {
            (firSymbol.fir.getContainingFile()?.psi as? KtFile)
                ?.takeUnless { it.isScript() }
                ?.javaFileFacadeFqName?.asString()
        } else {
            val classId = (containingSymbolOrSelf as? KaConstructorSymbol)?.containingClassId
                ?: containingSymbolOrSelf.callableId?.classId
            classId?.takeUnless { it.shortClassName.isSpecial }
                ?.asFqNameString()
        }
    }

    override fun getContainingModule(symbol: KaSymbol): KtModule {
        return symbol.getContainingKtModule(analysisSession.firResolveSession)
    }

    private fun getContainingPsi(symbol: KaSymbol): KtDeclaration? {
        val source = symbol.firSymbol.source
            ?: errorWithAttachment("PSI should present for declaration built by Kotlin code") {
                withSymbolAttachment("symbolForContainingPsi", analysisSession, symbol)
            }

        getContainingPsiForFakeSource(source)?.let { return it }

        val psi = source.psi
            ?: errorWithAttachment("PSI not found for source kind '${source.kind}'") {
                withSymbolAttachment("symbolForContainingPsi", analysisSession, symbol)
            }

        if (source.kind != KtRealSourceElementKind) {
            errorWithAttachment("Cannot compute containing PSI for unknown source kind '${source.kind}' (${psi::class.simpleName})") {
                withSymbolAttachment("symbolForContainingPsi", analysisSession, symbol)
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
                    withSymbolAttachment("symbolForContainingPsi", analysisSession, symbol)
                }
            }

            return result
        }

        errorWithAttachment("Unsupported declaration origin ${symbol.origin} ${psi::class}") {
            withSymbolAttachment("symbolForContainingPsi", analysisSession, symbol)
        }
    }

    private fun hasParentPsi(symbol: KaSymbol): Boolean {
        val source = symbol.firSymbol.source?.takeIf { it.psi is KtElement } ?: return false

        return getContainingPsiForFakeSource(source) != null
                || isSyntheticSymbolWithParentSource(symbol)
                || isOrdinarySymbolWithSource(symbol)
    }

    private fun isSyntheticSymbolWithParentSource(symbol: KaSymbol): Boolean {
        return when (symbol.origin) {
            KaSymbolOrigin.SOURCE_MEMBER_GENERATED -> true
            else -> false
        }
    }

    private fun isOrdinarySymbolWithSource(symbol: KaSymbol): Boolean {
        return symbol.origin == KaSymbolOrigin.SOURCE
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
