/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirEnumEntryInitializerSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirFileSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.getContainingKtModule
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForLocalAttr
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.resolve.calls.FirSimpleSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirDanglingModifierSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

context(relationProvider: KaFirSymbolRelationProvider)
internal fun computeContainingSymbol(symbol: KaSymbol): KaSymbol? = when (symbol) {
    is KaPackageSymbol -> null
    is KaFileSymbol -> analysisSession.firSymbolBuilder.createPackageSymbol(kotlinPackageFqn)
    else -> relationProvider.containingDeclaration(symbol) ?: relationProvider.containingFile(symbol)
}

context(relationProvider: KaFirSymbolRelationProvider)
internal fun computeContainingDeclaration(symbol: KaSymbol): KaDeclarationSymbol? {
    if (!hasParentSymbol(symbol)) {
        return null
    }

    getContainingDeclarationForDependentDeclaration(symbol)?.let { return it }

    // Handle intersection overrides on synthetic properties
    val firSymbol = (symbol.firSymbol as? FirSimpleSyntheticPropertySymbol)?.getterSymbol?.delegateFunctionSymbol
        ?: symbol.firSymbol
    val symbolFirSession = firSymbol.llFirSession
    val symbolModule = symbolFirSession.ktModule

    if (symbolModule is KaDanglingFileModule && symbolModule.resolutionMode == KaDanglingFileResolutionMode.IGNORE_SELF) {
        if (hasParentPsi(symbol)) {
            // getSymbol(ClassId) returns a symbol from the original file, so here we avoid using it
            return getContainingDeclarationByPsi(symbol)
        }
    }

    when (symbol) {
        is KaLocalVariableSymbol,
        is KaAnonymousFunctionSymbol,
        is KaAnonymousObjectSymbol,
        is KaDestructuringDeclarationSymbol,
            -> {
            return getContainingDeclarationByPsi(symbol)
        }

        is KaClassInitializerSymbol -> {
            val outerFirClassifier = firSymbol.getContainingClassSymbol()
            if (outerFirClassifier != null) {
                return firSymbolBuilder.buildSymbol(outerFirClassifier) as? KaDeclarationSymbol
            }
        }

        is KaCallableSymbol -> {
            val typeAliasForConstructor = (firSymbol as? FirConstructorSymbol)?.typeAliasConstructorInfo?.typeAliasSymbol
            if (typeAliasForConstructor != null) {
                return firSymbolBuilder.classifierBuilder.buildTypeAliasSymbol(typeAliasForConstructor)
            }

            val outerFirClassifier = firSymbol.getContainingClassSymbol()
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
            firSymbol.getContainingClassSymbol()?.let { outerFirClassifier ->
                return firSymbolBuilder.buildSymbol(outerFirClassifier) as? KaDeclarationSymbol
            }
            getContainingDeclarationsForLocalClass(firSymbol, symbolFirSession)?.let { return it }
        }
    }

    return getContainingDeclarationByPsi(symbol)
}

context(relationProvider: KaFirSymbolRelationProvider)
private fun getContainingDeclarationsForLocalClass(firSymbol: FirBasedSymbol<*>, symbolFirSession: FirSession): KaDeclarationSymbol? {
    val fir = firSymbol.fir as? FirClassLikeDeclaration ?: return null
    val containerSymbol = fir.containingClassForLocalAttr?.toSymbol(symbolFirSession) ?: return null
    return firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(containerSymbol)
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

    if (symbol.isTopLevel) {
        // PSI fast path without building FIR
        val containingKtFile = (symbol.realPsi as? KtDeclaration)?.containingKtFile
        if (containingKtFile != null && !containingKtFile.isScript()) {
            return false
        }

        val containingFile = (symbol.firSymbol.fir as? FirElementWithResolveState)?.getContainingFile()

        @OptIn(DirectDeclarationsAccess::class)
        if (containingFile == null || containingFile.declarations.firstOrNull() !is FirScript) {
            // Should be replaced with proper check after KT-61451 and KT-61887
            return false
        }
    }

    return when (symbol) {
        is KaKotlinPropertySymbol, is KaLocalVariableSymbol -> symbol.firSymbol.origin != FirDeclarationOrigin.ForeignValue
        else -> true
    }
}

context(relationProvider: KaFirSymbolRelationProvider)
private fun getContainingDeclarationByPsi(symbol: KaSymbol): KaDeclarationSymbol? {
    val containingDeclaration = getContainingPsi(symbol) ?: return null
    val declarationSymbol = with(analysisSession) { containingDeclaration.symbol }

    if (declarationSymbol is KaFirEnumEntrySymbol && symbol !is KaFirEnumEntryInitializerSymbol) {
        // From the FIR point of view, the real containing declaration of enum entry functions or implicit constructor
        // is not the enum entry itself, but its `KaFirEnumEntryInitializerSymbol`.
        return declarationSymbol.initializer
    }

    return declarationSymbol
}

context(relationProvider: KaFirSymbolRelationProvider)
private fun getContainingDeclarationForDependentDeclaration(symbol: KaSymbol): KaDeclarationSymbol? = when (symbol) {
    is KaReceiverParameterSymbol -> symbol.owningCallableSymbol
    is KaBackingFieldSymbol -> symbol.owningProperty
    is KaPropertyAccessorSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.propertySymbol) as KaDeclarationSymbol
    is KaTypeParameterSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.containingDeclarationSymbol) as? KaDeclarationSymbol
    is KaValueParameterSymbol -> firSymbolBuilder.buildSymbol(symbol.firSymbol.containingDeclarationSymbol) as? KaDeclarationSymbol
    is KaContextParameterSymbol -> {
        val containingFirSymbol = symbol.firSymbol.containingDeclarationSymbol
        val firSymbol = if (containingFirSymbol is FirDanglingModifierSymbol) {
            containingFirSymbol.getContainingClassSymbol()
                ?: containingFirSymbol.fir.getContainingFile()?.symbol
                ?: errorWithAttachment("Containing element is expected for the dangling modifier symbol") {
                    withSymbolAttachment("symbolForContainingPsi", analysisSession, symbol)
                    withFirSymbolEntry("containingFirSymbol", containingFirSymbol)
                }
        } else {
            containingFirSymbol
        }

        firSymbolBuilder.buildSymbol(firSymbol) as? KaDeclarationSymbol
    }
    else -> null
}

context(relationProvider: KaFirSymbolRelationProvider)
internal fun computeContainingFile(symbol: KaSymbol): KaFileSymbol? {
    if (symbol is KaFileSymbol || symbol is KaPackageSymbol) {
        return null
    }

    when (val psi = symbol.realPsi) {
        null -> {}

        // Library declarations might belong to either a deserialized or a resolvable library session, so only FIR can tell
        is KtElement -> psi.containingKtFile.takeUnless(KtFile::isCompiled)?.let {
            return KaFirFileSymbol(it, analysisSession)
        }

        // Java declarations
        else -> return null
    }

    val firFileSymbol = symbol.firSymbol.fir.getContainingFile()?.symbol ?: return null
    return firSymbolBuilder.buildFileSymbol(firFileSymbol)
}

context(relationProvider: KaFirSymbolRelationProvider)
internal fun computeContainingModule(symbol: KaSymbol): KaModule = when (symbol) {
    is KaFirSymbol<*> -> symbol.firSymbol.getContainingKtModule(resolutionFacade)
    is KaPackageSymbol -> analysisSession.useSiteModule
    else -> errorWithAttachment("Unsupported symbol type: ${relationProvider::class.simpleName}") {
        withSymbolAttachment("symbol", analysisSession, symbol)
    }
}

context(relationProvider: KaFirSymbolRelationProvider)
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

            if (psi.parentOfType<KtModifierList>() != null) {
                // Invalid code: the declaration is nested inside a dangling annotation
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
        // A property synthesized from a constructor parameter is still a class member, not a constructor member.
        KtFakeSourceElementKind.PropertyFromParameter -> source.psi?.parentOfType<KtClassOrObject>()!!
        KtFakeSourceElementKind.EnumInitializer -> source.psi as KtEnumEntry
        is KtFakeSourceElementKind.EnumGeneratedDeclaration -> source.psi as KtDeclaration
        is KtFakeSourceElementKind.ScriptParameter -> source.psi as KtScript
        is KtFakeSourceElementKind.DataClassGeneratedMembers -> when (val source = source.psi) {
            is KtClassOrObject -> {
                // for generated `equals`, `hashCode`, `toString` methods the source is the containing `KtClass`
                source
            }
            is KtParameter -> {
                // for `componentN` functions, the source points to the parameter by which the `componentN` function was generated
                val constructor = source.ownerFunction as KtPrimaryConstructor
                constructor.containingClassOrObject!!
            }
            is KtPrimaryConstructor -> {
                source.containingClassOrObject!!
            }
            else -> null
        }
        else -> null
    }
}

private fun PsiElement.getContainingPsiDeclaration(): KtDeclaration? {
    for (parent in parents) {
        if (parent is KtDeclaration && parent !is KtDestructuringDeclaration) {
            return parent
        }
    }

    return null
}
