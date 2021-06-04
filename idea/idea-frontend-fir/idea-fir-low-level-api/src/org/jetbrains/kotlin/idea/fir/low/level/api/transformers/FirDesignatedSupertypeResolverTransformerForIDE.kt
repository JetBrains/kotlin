/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.collectDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.runCustomResolveUnderLock
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.isResolvedForAllDeclarations
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.updateResolvedPhaseForDeclarationAndChildren
import org.jetbrains.kotlin.idea.fir.low.level.api.util.checkCanceled
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensurePhase

/**
 * Transform designation into SUPER_TYPES phase. Affects only for designation, target declaration, it's children and dependents
 */
internal class FirDesignatedSupertypeResolverTransformerForIDE(
    private val designation: FirDeclarationDesignationWithFile,
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    private val declarationPhaseDowngraded: Boolean,
    private val moduleFileCache: ModuleFileCache,
    private val firLazyDeclarationResolver: FirLazyDeclarationResolver,
    private val firProviderInterceptor: FirProviderInterceptor?,
    private val checkPCE: Boolean,
) : FirLazyTransformerForIDE {

    private val supertypeComputationSession = SupertypeComputationSession()

    private inner class DesignatedFirSupertypeResolverVisitor(classDesignation: FirDeclarationDesignation) :
        FirSupertypeResolverVisitor(
            session = session,
            supertypeComputationSession = supertypeComputationSession,
            scopeSession = scopeSession,
            scopeForLocalClass = null,
            localClassesNavigationInfo = null,
            firProviderInterceptor = firProviderInterceptor,
        ) {
        val declarationTransformer = IDEDeclarationTransformer(classDesignation)

        override fun visitDeclarationContent(declaration: FirDeclaration, data: Any?) {
            declarationTransformer.visitDeclarationContent(this, declaration, data) {
                super.visitDeclarationContent(declaration, data)
                declaration
            }
        }
    }

    private inner class DesignatedFirApplySupertypesTransformer(classDesignation: FirDeclarationDesignation) :
        FirApplySupertypesTransformer(supertypeComputationSession) {

        override fun needReplacePhase(firDeclaration: FirDeclaration): Boolean =
            firDeclaration !is FirFile && super.needReplacePhase(firDeclaration)

        override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): FirStatement {
            return if (regularClass.resolvePhase >= FirResolvePhase.SUPER_TYPES)
                transformDeclarationContent(regularClass, data) as FirStatement
            else super.transformRegularClass(regularClass, data)
        }

        override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): FirStatement {
            return if (anonymousObject.resolvePhase >= FirResolvePhase.SUPER_TYPES)
                transformDeclarationContent(anonymousObject, data) as FirStatement
            else super.transformAnonymousObject(anonymousObject, data)
        }

        override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Any?): FirDeclaration {
            return if (typeAlias.resolvePhase >= FirResolvePhase.SUPER_TYPES)
                transformDeclarationContent(typeAlias, data)
            else super.transformTypeAlias(typeAlias, data)
        }

        val declarationTransformer = IDEDeclarationTransformer(classDesignation)

        override fun transformDeclarationContent(declaration: FirDeclaration, data: Any?): FirDeclaration {
            return declarationTransformer.transformDeclarationContent(this, declaration, data) {
                super.transformDeclarationContent(declaration, data)
            }
        }
    }

    private fun collect(designation: FirDeclarationDesignationWithFile): Collection<FirDeclarationDesignationWithFile> {
        val visited = mutableMapOf<FirDeclaration, FirDeclarationDesignationWithFile>()
        val toVisit = mutableListOf<FirDeclarationDesignationWithFile>()

        toVisit.add(designation)
        while (toVisit.isNotEmpty()) {
            for (nowVisit in toVisit) {
                if (checkPCE) checkCanceled()
                val resolver = DesignatedFirSupertypeResolverVisitor(nowVisit)
                moduleFileCache.firFileLockProvider.runCustomResolveUnderLock(nowVisit.firFile, checkPCE) {
                    firLazyDeclarationResolver.lazyResolveFileDeclaration(
                        firFile = nowVisit.firFile,
                        moduleFileCache = moduleFileCache,
                        toPhase = FirResolvePhase.IMPORTS,
                        scopeSession = scopeSession,
                        checkPCE = true,
                    )
                    nowVisit.firFile.accept(resolver, null)
                }
                resolver.declarationTransformer.ensureDesignationPassed()
                visited[nowVisit.declaration] = nowVisit
            }
            toVisit.clear()

            for (value in supertypeComputationSession.supertypeStatusMap.values) {
                if (value !is SupertypeComputationStatus.Computed) continue
                for (reference in value.supertypeRefs) {
                    val classLikeDeclaration = reference.type.toSymbol(session)?.fir
                    if (classLikeDeclaration !is FirClassLikeDeclaration<*>) continue
                    if (classLikeDeclaration is FirJavaClass) continue
                    if (visited.containsKey(classLikeDeclaration)) continue
                    val containingFile = moduleFileCache.getContainerFirFile(classLikeDeclaration) ?: continue
                    toVisit.add(classLikeDeclaration.collectDesignation(containingFile))
                }
            }
        }
        return visited.values
    }

    private fun apply(visited: Collection<FirDeclarationDesignationWithFile>) {
        fun applyToFileSymbols(designations: List<FirDeclarationDesignationWithFile>) {
            for (designation in designations) {
                if (checkPCE) checkCanceled()
                val applier = DesignatedFirApplySupertypesTransformer(designation)
                designation.firFile.transform<FirElement, Void?>(applier, null)
                applier.declarationTransformer.ensureDesignationPassed()
            }
        }

        val filesToDesignations = visited.groupBy { it.firFile }
        for (designationsPerFile in filesToDesignations) {
            if (checkPCE) checkCanceled()
            moduleFileCache.firFileLockProvider.runCustomResolveUnderLock(designationsPerFile.key, checkPCE) {
                applyToFileSymbols(designationsPerFile.value)
            }
        }
    }

    override fun transformDeclaration(phaseRunner: FirPhaseRunner) {
        check(designation.firFile.resolvePhase >= FirResolvePhase.IMPORTS) {
            "Invalid resolve phase of file. Should be IMPORTS but found ${designation.firFile.resolvePhase}"
        }

        val targetDesignation = if (designation.declaration !is FirClassLikeDeclaration<*>) {
            val resolvableTarget = designation.path.lastOrNull() ?: return
            check(resolvableTarget is FirClassLikeDeclaration<*>)
            val targetPath = designation.path.dropLast(1)
            FirDeclarationDesignationWithFile(targetPath, resolvableTarget, designation.firFile)
        } else designation

        if (targetDesignation.isResolvedForAllDeclarations(FirResolvePhase.SUPER_TYPES, declarationPhaseDowngraded)) return
        targetDesignation.declaration.updateResolvedPhaseForDeclarationAndChildren(FirResolvePhase.SUPER_TYPES)

        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.SUPER_TYPES) {
            val collected = collect(targetDesignation)
            supertypeComputationSession.breakLoops(session)
            apply(collected)
        }

        designation.path.forEach(::ensureResolved)
        ensureResolved(designation.declaration)
        ensureResolvedDeep(designation.declaration)
    }

    override fun ensureResolved(declaration: FirDeclaration) {
        when (declaration) {
            is FirFunction<*>, is FirProperty, is FirEnumEntry, is FirField, is FirAnonymousInitializer -> Unit
            is FirRegularClass -> {
                declaration.ensurePhase(FirResolvePhase.SUPER_TYPES)
                check(declaration.superTypeRefs.all { it is FirResolvedTypeRef })
            }
            is FirTypeAlias -> {
                declaration.ensurePhase(FirResolvePhase.SUPER_TYPES)
                check(declaration.expandedTypeRef is FirResolvedTypeRef)
            }
            else -> error("Unexpected type: ${declaration::class.simpleName}")
        }
    }
}
