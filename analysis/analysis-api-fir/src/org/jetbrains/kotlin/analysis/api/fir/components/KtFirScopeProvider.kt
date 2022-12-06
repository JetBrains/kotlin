/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.components.KtImplicitReceiver
import org.jetbrains.kotlin.analysis.api.components.KtScopeContext
import org.jetbrains.kotlin.analysis.api.components.KtScopeProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.scopes.*
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirFileSymbol
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KtCompositeScope
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KtEmptyScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtTypeScope
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.utils.printer.getElementTextInContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.delegateFields
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.analysis.api.fir.scopes.JavaClassDeclaredMembersEnhancementScope
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.scopeSessionKey
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

internal class KtFirScopeProvider(
    override val analysisSession: KtFirAnalysisSession,
    private val builder: KtSymbolByFirBuilder,
    private val project: Project,
    private val firResolveSession: LLFirResolveSession,
) : KtScopeProvider() {

    private fun getScopeSession(): ScopeSession {
        return analysisSession.getScopeSessionFor(analysisSession.useSiteSession)
    }

    private inline fun <T> KtSymbolWithMembers.withFirForScope(crossinline body: (FirClass) -> T): T? {
        return when (this) {
            is KtFirNamedClassOrObjectSymbol -> body(firSymbol.fir)
            is KtFirAnonymousObjectSymbol -> body(firSymbol.fir)
            is KtFirEnumEntrySymbol -> {
                firSymbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                val initializer = firSymbol.fir.initializer ?: return null
                check(initializer is FirAnonymousObjectExpression) { "Unexpected enum entry initializer: ${initializer.javaClass}" }
                body(initializer.anonymousObject)
            }

            else -> error { "Unknown KtSymbolWithDeclarations implementation ${this::class.qualifiedName}" }
        }
    }

    override fun getMemberScope(classSymbol: KtSymbolWithMembers): KtScope {
        val firScope = classSymbol.withFirForScope { fir ->
            fir.lazyResolveToPhase(FirResolvePhase.STATUS)
            val firSession = analysisSession.useSiteSession
            fir.unsubstitutedScope(
                firSession,
                getScopeSession(),
                withForcedTypeCalculator = false
            )
        }?.applyIf(classSymbol is KtEnumEntrySymbol, ::EnumEntryContainingNamesAwareScope)
            ?: return getEmptyScope()
        return KtFirDelegatingScope(firScope, builder)
    }

    override fun getStaticMemberScope(symbol: KtSymbolWithMembers): KtScope {
        val firScope = symbol.withFirForScope { fir ->
            val firSession = analysisSession.useSiteSession
            fir.scopeProvider.getStaticScope(
                fir,
                firSession,
                getScopeSession(),
            )
        } ?: return getEmptyScope()
        return KtFirDelegatingScope(firScope, builder)
    }

    override fun getDeclaredMemberScope(classSymbol: KtSymbolWithMembers): KtScope {
        val firScope = classSymbol.withFirForScope {
            val useSiteSession = analysisSession.useSiteSession
            when (val regularClass = classSymbol.firSymbol.fir) {
                is FirJavaClass -> buildJavaEnhancementDeclaredMemberScope(useSiteSession, regularClass.symbol, getScopeSession())
                else -> useSiteSession.declaredMemberScope(it)
            }
        } ?: return getEmptyScope()
        return KtFirDelegatingScope(firScope, builder)
    }

    override fun getDelegatedMemberScope(classSymbol: KtSymbolWithMembers): KtScope {
        val declaredScope = (getDeclaredMemberScope(classSymbol) as? KtFirDelegatingScope)?.firScope
            ?: return getEmptyScope()
        val firScope = classSymbol.withFirForScope { fir ->
            fir.lazyResolveToPhase(FirResolvePhase.STATUS)
            val delegateFields = fir.delegateFields
            if (delegateFields.isNotEmpty()) {
                val firSession = analysisSession.useSiteSession
                FirDelegatedMemberScope(
                    firSession,
                    getScopeSession(),
                    fir,
                    declaredScope,
                    delegateFields
                )
            } else null
        } ?: return getEmptyScope()

        return KtFirDelegatedMemberScope(firScope, builder)
    }

    override fun getFileScope(fileSymbol: KtFileSymbol): KtScope {
            check(fileSymbol is KtFirFileSymbol) { "KtFirScopeProvider can only work with KtFirFileSymbol, but ${fileSymbol::class} was provided" }
        return KtFirFileScope(fileSymbol, builder)
    }

    override fun getEmptyScope(): KtScope {
        return KtEmptyScope(token)
    }

    override fun getPackageScope(packageSymbol: KtPackageSymbol): KtScope {
        return createPackageScope(packageSymbol.fqName)
    }


    override fun getCompositeScope(subScopes: List<KtScope>): KtScope {
        return KtCompositeScope(subScopes, token)
    }

    override fun getTypeScope(type: KtType): KtTypeScope? = getFirTypeScope(type)?.let { convertToKtTypeScope(it) }

    override fun getSyntheticJavaPropertiesScope(type: KtType): KtTypeScope? {
        check(type is KtFirType) { "KtFirScopeProvider can only work with KtFirType, but ${type::class} was provided" }
        val firTypeScope = getFirTypeScope(type) ?: return null
        return FirSyntheticPropertiesScope.createIfSyntheticNamesProviderIsDefined(
            firResolveSession.useSiteFirSession,
            type.coneType,
            firTypeScope
        )?.let { convertToKtTypeScope(it) }
    }

    override fun getScopeContextForPosition(
        originalFile: KtFile,
        positionInFakeFile: KtElement
    ): KtScopeContext {
        val towerDataContext =
            analysisSession.firResolveSession.getTowerContextProvider(originalFile).getClosestAvailableParentContext(positionInFakeFile)
                ?: error("Cannot find enclosing declaration for ${positionInFakeFile.getElementTextInContext()}")

        val implicitReceivers = towerDataContext.nonLocalTowerDataElements.mapNotNull { it.implicitReceiver }.distinct()
        val implicitKtReceivers = implicitReceivers.map { receiver ->
            KtImplicitReceiver(
                token,
                builder.typeBuilder.buildKtType(receiver.type),
                builder.buildSymbol(receiver.boundSymbol.fir),
            )
        }

        val implicitReceiverScopes = implicitReceivers.mapNotNull { it.implicitScope }
        val nonLocalScopes = towerDataContext.nonLocalTowerDataElements.mapNotNull { it.scope }.distinct()
        val firLocalScopes = towerDataContext.localScopes

        val allKtScopes = buildList<KtScope> {
            implicitReceiverScopes.mapTo(this, ::convertToKtScope)
            nonLocalScopes.mapTo(this, ::convertToKtScope)
            firLocalScopes.mapTo(this, ::convertToKtScope)
        }

        return KtScopeContext(
            getCompositeScope(allKtScopes.asReversed()),
            implicitKtReceivers.asReversed(),
            token
        )
    }

    private fun convertToKtScope(firScope: FirScope): KtScope {
        return when (firScope) {
            is FirAbstractSimpleImportingScope -> KtFirNonStarImportingScope(firScope, builder)
            is FirAbstractStarImportingScope -> KtFirStarImportingScope(firScope, builder, analysisSession.useSiteScopeDeclarationProvider)
            is FirPackageMemberScope -> createPackageScope(firScope.fqName)
            is FirContainingNamesAwareScope -> KtFirDelegatingScope(firScope, builder)
            else -> TODO(firScope::class.toString())
        }
    }

    private fun createPackageScope(fqName: FqName): KtFirPackageScope {
        return KtFirPackageScope(
            fqName,
            project,
            builder,
            analysisSession.useSiteAnalysisScope,
            analysisSession.useSiteScopeDeclarationProvider,
            analysisSession.targetPlatform
        )
    }

    private fun convertToKtTypeScope(firScope: FirScope): KtTypeScope {
        return when (firScope) {
            is FirContainingNamesAwareScope -> KtFirDelegatingTypeScope(firScope, builder)
            else -> TODO(firScope::class.toString())
        }
    }

    private fun getFirTypeScope(type: KtType): FirTypeScope? {
        check(type is KtFirType) { "KtFirScopeProvider can only work with KtFirType, but ${type::class} was provided" }
        return type.coneType.scope(
            firResolveSession.useSiteFirSession,
            getScopeSession(),
            FakeOverrideTypeCalculator.Forced,
            requiredPhase = FirResolvePhase.STATUS,
        )
    }

    private fun buildJavaEnhancementDeclaredMemberScope(useSiteSession: FirSession, symbol: FirRegularClassSymbol, scopeSession: ScopeSession): JavaClassDeclaredMembersEnhancementScope {
        return scopeSession.getOrBuild(symbol, JAVA_ENHANCEMENT_FOR_DECLARED_MEMBER) {
            val firJavaClass = symbol.fir
            require(firJavaClass is FirJavaClass) {
                "${firJavaClass.classId} is expected to be FirJavaClass, but ${firJavaClass::class} found"
            }
            JavaClassDeclaredMembersEnhancementScope(
                useSiteSession,
                firJavaClass,
                JavaScopeProvider.getUseSiteMemberScope(firJavaClass, useSiteSession, scopeSession)
            )
        }
    }
}

private class EnumEntryContainingNamesAwareScope(private val originalScope: FirContainingNamesAwareScope) : FirContainingNamesAwareScope() {
    override fun getCallableNames(): Set<Name> = originalScope.getCallableNames()
    override fun getClassifierNames(): Set<Name> = originalScope.getClassifierNames()
    override fun mayContainName(name: Name): Boolean = originalScope.mayContainName(name)
    override val scopeOwnerLookupNames: List<String> get() = super.scopeOwnerLookupNames

    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        originalScope.processClassifiersByNameWithSubstitution(name) { classifier, substitutor ->
            if ((classifier as? FirRegularClassSymbol)?.isCompanion != true) {
                processor(classifier, substitutor)
            }
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        originalScope.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        originalScope.processPropertiesByName(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        // enum entries does not have constructors
    }
}

private val JAVA_ENHANCEMENT_FOR_DECLARED_MEMBER = scopeSessionKey<FirRegularClassSymbol, JavaClassDeclaredMembersEnhancementScope>()
