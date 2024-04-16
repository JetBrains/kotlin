/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirKotlinSymbolProvider
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.getDeprecationsProvider
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.name.NativeStandardInteropNames
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

internal class IdeForwardDeclarationsSymbolProvider(
    project: Project,
    session: FirSession,
    moduleDataProvider: SingleModuleDataProvider,
    private val kotlinScopeProvider: FirKotlinScopeProvider,
    scope: GlobalSearchScope,
) : LLFirKotlinSymbolProvider(
    session,
) {
    private val moduleData = moduleDataProvider.getModuleData(null)
    private val stubBasedSymbolProviderHelper =
        StubBasedSymbolProviderHelper(project, scope, session, isFallbackDependenciesProvider = false)

    override val declarationProvider
        get() = stubBasedSymbolProviderHelper.declarationProvider
    override val packageProvider
        get() = stubBasedSymbolProviderHelper.packageProvider
    override val allowKotlinPackage: Boolean
        get() = true
    override val symbolNamesProvider: FirSymbolNamesProvider
        get() = stubBasedSymbolProviderHelper.symbolNamesProvider

    private val classCache: FirCache<ClassId, FirRegularClassSymbol?, Unit?> =
        session.firCachesFactory.createCache(
            createValue = { classId, _ -> createClassLikeSymbolByClassId(classId) }
        )

    override fun getPackage(fqName: FqName): FqName? = fqName.takeIf { packageProvider.doesKotlinOnlyPackageExist(fqName) }

    @FirSymbolProviderInternals
    override fun getClassLikeSymbolByClassId(
        classId: ClassId,
        classLikeDeclaration: KtClassLikeDeclaration,
    ): FirClassLikeSymbol<*>? {
        return classCache.getValue(classId, Unit)
    }

    private fun createClassLikeSymbolByClassId(
        classId: ClassId,
    ): FirRegularClassSymbol? {
        val classLikeDeclaration: KtClassLikeDeclaration = declarationProvider.getClassLikeDeclarationByClassId(classId) ?: return null
        val forwardDeclarationKind = NativeForwardDeclarationKind.packageFqNameToKind[classId.packageFqName] ?: return null

        val symbol = FirRegularClassSymbol(classId)

        buildRegularClass {
            moduleData = this@IdeForwardDeclarationsSymbolProvider.moduleData
            source = KtRealPsiSourceElement(classLikeDeclaration)
            origin = FirDeclarationOrigin.Synthetic.ForwardDeclaration
            check(!classId.isNestedClass) { "Expected top-level class when building forward declaration, got $classId" }
            name = classId.shortClassName
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            ).apply {
                // This will be wrong if we support exported forward declarations.
                // See https://youtrack.jetbrains.com/issue/KT-51377 for more details.
                isExpect = false

                isActual = false
                isCompanion = false
                isInner = false
                isData = false
                isInline = false
                isExternal = false
                isFun = false
            }
            classKind = forwardDeclarationKind.classKind
            scopeProvider = kotlinScopeProvider
            this.symbol = symbol

            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

            superTypeRefs += buildResolvedTypeRef {
                type = ConeClassLikeLookupTagImpl(forwardDeclarationKind.superClassId)
                    .constructClassType(emptyArray(), isNullable = false)
            }

            annotations += buildAnnotation {
                annotationTypeRef = buildResolvedTypeRef {
                    val annotationClassId = ClassId(
                        NativeStandardInteropNames.cInteropPackage,
                        NativeStandardInteropNames.ExperimentalForeignApi
                    )
                    type = ConeClassLikeLookupTagImpl(annotationClassId)
                        .constructClassType(typeArguments = ConeTypeProjection.EMPTY_ARRAY, isNullable = false)
                }
                argumentMapping = FirEmptyAnnotationArgumentMapping
            }
        }.apply {
            replaceDeprecationsProvider(getDeprecationsProvider(session))
        }

        return symbol
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        val declaration = declarationProvider.getClassLikeDeclarationByClassId(classId) ?: return null
        @OptIn(FirSymbolProviderInternals::class)
        return getClassLikeSymbolByClassId(classId, declaration)
    }

    // Region: no-op overrides for symbols that don't exist in K/N forward declarations

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<FirCallableSymbol<*>>,
        callableId: CallableId,
        callables: Collection<KtCallableDeclaration>,
    ) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<FirNamedFunctionSymbol>,
        callableId: CallableId,
        functions: Collection<KtNamedFunction>,
    ) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<FirPropertySymbol>,
        callableId: CallableId,
        properties: Collection<KtProperty>,
    ) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<FirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<FirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<FirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    // endregion
}

/*
internal class IdeForwardDeclarationsSymbolProvider(
    firSession: FirSession,
    private val project: Project,
    private val scope: GlobalSearchScope,
    private val firFileBuilder: LLFirFileBuilder,
) : LLFirKotlinSymbolProvider(firSession) {
    override val declarationProvider = project.createDeclarationProvider(
        scope,
        contextualModule = firSession.llFirModuleData.ktModule,
    )
    override val packageProvider: KotlinPackageProvider
        get() = session.llFirModuleData.ktModule.project.createPackageProvider(scope)

    override val symbolNamesProvider: FirSymbolNamesProvider = LLFirKotlinSymbolNamesProvider.cached(firSession, declarationProvider)

    private val classifierByClassId =
        firSession.firCachesFactory.createCache<ClassId, FirClassLikeDeclaration?, KtClassLikeDeclaration?> { classId, context ->
            require(context == null || context.isPhysical)
            val ktClass = context ?: declarationProvider.getClassLikeDeclarationByClassId(classId) ?: return@createCache null

            if (ktClass.getClassId() == null) return@createCache null
            val firFile = firFileBuilder.buildRawFirFileWithCaching(ktClass.containingKtFile)
            FirElementFinder.findClassifierWithClassId(firFile, classId)
                ?: errorWithAttachment("Classifier was found in KtFile but was not found in FirFile") {
                    withEntry("classifierClassId", classId) { it.asString() }
                    withVirtualFileEntry("virtualFile", ktClass.containingKtFile.virtualFile)
                }
        }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        return classifierByClassId.getValue(classId, null)?.symbol
    }

    @FirSymbolProviderInternals
    override fun getClassLikeSymbolByClassId(
        classId: ClassId,
        classLikeDeclaration: KtClassLikeDeclaration,
    ): FirClassLikeSymbol<*>? {
        return classifierByClassId.getValue(classId, classLikeDeclaration)?.symbol
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<FirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<FirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<FirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    override fun getPackage(fqName: FqName): FqName? {
        return fqName.takeIf { packageProvider.doesKotlinOnlyPackageExist(fqName) }
    }

    override val allowKotlinPackage: Boolean get() = true

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<FirCallableSymbol<*>>,
        callableId: CallableId,
        callables: Collection<KtCallableDeclaration>,
    ) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<FirNamedFunctionSymbol>,
        callableId: CallableId,
        functions: Collection<KtNamedFunction>,
    ) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<FirPropertySymbol>,
        callableId: CallableId,
        properties: Collection<KtProperty>,
    ) {
    }

}*/
