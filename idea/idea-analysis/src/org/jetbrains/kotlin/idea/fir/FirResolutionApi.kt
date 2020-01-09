/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.getClassDeclaredCallableSymbols
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDesignatedBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.runResolve
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.util.containingNonLocalDeclaration

private val FirResolvePhase.stubMode: Boolean
    get() = this <= FirResolvePhase.DECLARATIONS

private fun KtClassOrObject.relativeFqName(): FqName {
    val className = this.nameAsSafeName
    val parentFqName = this.containingClassOrObject?.relativeFqName()
    return parentFqName?.child(className) ?: FqName.topLevel(className)
}

private fun FirFile.findCallableMember(
    provider: FirProvider, callableMember: KtCallableDeclaration,
    packageFqName: FqName, klassFqName: FqName?, declName: Name
): FirCallableDeclaration<*> {
    if (klassFqName != null) {
        return provider.getClassDeclaredCallableSymbols(ClassId(packageFqName, klassFqName, false), declName)
            .find { symbol: FirCallableSymbol<*> ->
                symbol.fir.psi == callableMember
            }?.fir ?: error("Cannot find FIR callable declaration ${CallableId(packageFqName, klassFqName, declName)}")
    }
    // NB: not sure it's correct to use member scope provider from here (because of possible changes)
    val memberScope = FirPackageMemberScope(this.packageFqName, session)
    var result: FirCallableDeclaration<*>? = null
    val processor = { symbol: FirCallableSymbol<*> ->
        val fir = symbol.fir
        if (result == null && fir.psi == callableMember) {
            result = fir
        }
    }
    if (callableMember is KtNamedFunction || callableMember is KtConstructor<*>) {
        memberScope.processFunctionsByName(declName, processor)
    } else {
        memberScope.processPropertiesByName(declName, processor)
    }

    return result
        ?: error("Cannot find FIR callable declaration ${CallableId(packageFqName, klassFqName, declName)}")
}

fun KtCallableDeclaration.getOrBuildFir(
    state: FirModuleResolveState,
    phase: FirResolvePhase = FirResolvePhase.DECLARATIONS
): FirCallableDeclaration<*> {
    val session = state.getSession(this)

    val file = this.containingKtFile
    val packageFqName = file.packageFqName
    val klassFqName = this.containingClassOrObject?.relativeFqName()
    val declName = this.nameAsSafeName

    val firProvider = FirProvider.getInstance(session) as FirIdeProvider
    val firFile = firProvider.getOrBuildFile(file)
    val firMemberSymbol = firFile.findCallableMember(firProvider, this, packageFqName, klassFqName, declName).symbol
    val firMemberDeclaration = firMemberSymbol.fir
    if (firMemberDeclaration.resolvePhase >= phase) {
        return firMemberDeclaration
    }
    synchronized(firFile) {
        firMemberDeclaration.runResolve(firFile, firProvider, phase, state)
    }
    return firMemberDeclaration
}

fun KtClassOrObject.getOrBuildFir(
    state: FirModuleResolveState,
    phase: FirResolvePhase = FirResolvePhase.DECLARATIONS
): FirMemberDeclaration {
    val session = state.getSession(this)

    val file = this.containingKtFile
    val packageFqName = file.packageFqName
    val klassFqName = this.relativeFqName()

    val firProvider = FirProvider.getInstance(session) as FirIdeProvider
    val firFile = firProvider.getOrBuildFile(file)

    val firClassOrEnumEntry = if (this is KtEnumEntry) {
        val firEnumClass = firProvider.getFirClassifierByFqName(ClassId(packageFqName, klassFqName.parent(), false)) as FirRegularClass
        firEnumClass.declarations.first { it is FirEnumEntry && it.name == this.nameAsSafeName } as FirMemberDeclaration
    } else {
        firProvider.getFirClassifierByFqName(ClassId(packageFqName, klassFqName, false)) as FirRegularClass
    }
    if (firClassOrEnumEntry.resolvePhase >= phase) {
        return firClassOrEnumEntry
    }
    synchronized(firFile) {
        firClassOrEnumEntry.runResolve(firFile, firProvider, phase, state)
    }
    return firClassOrEnumEntry
}

private fun KtFile.getOrBuildRawFirFile(state: FirModuleResolveState): Pair<FirIdeProvider, FirFile> {
    val session = state.getSession(this)
    val firProvider = FirProvider.getInstance(session) as FirIdeProvider
    return firProvider to firProvider.getOrBuildFile(this)
}

fun KtFile.getOrBuildFir(
    state: FirModuleResolveState,
    phase: FirResolvePhase = FirResolvePhase.DECLARATIONS
): FirFile {
    val (firProvider, firFile) = getOrBuildRawFirFile(state)
    if (phase <= FirResolvePhase.DECLARATIONS && firFile.resolvePhase >= phase) {
        return firFile
    }
    synchronized(firFile) {
        firFile.runResolve(firFile, firProvider, phase, state)
    }
    return firFile
}

fun KtFile.getOrBuildFirWithDiagnostics(state: FirModuleResolveState): FirFile {
    val (_, firFile) = getOrBuildRawFirFile(state)
    val currentResolvePhase = firFile.resolvePhase
    if (currentResolvePhase < FirResolvePhase.BODY_RESOLVE) {
        synchronized(firFile) {
            firFile.runResolve(toPhase = FirResolvePhase.BODY_RESOLVE, fromPhase = currentResolvePhase)
        }
    }

    ProgressIndicatorProvider.checkCanceled() // ???
    if (state.hasDiagnosticsForFile(this)) return firFile

    FirIdeDiagnosticsCollector(state).collectDiagnostics(firFile)
    state.setDiagnosticsForFile(this, firFile)
    return firFile
}

private fun FirDeclaration.runResolve(
    file: FirFile,
    firProvider: FirIdeProvider,
    toPhase: FirResolvePhase,
    state: FirModuleResolveState
) {
    val nonLazyPhase = minOf(toPhase, FirResolvePhase.DECLARATIONS)
    file.runResolve(toPhase = nonLazyPhase, fromPhase = this.resolvePhase)
    if (toPhase <= nonLazyPhase) return
    val designation = mutableListOf<FirDeclaration>(file)
    if (this !is FirFile) {
        val id = when (this) {
            is FirCallableDeclaration<*> -> {
                this.symbol.callableId.classId
            }
            is FirRegularClass -> {
                this.symbol.classId
            }
            else -> error("Unsupported: ${render()}")
        }
        val outerClasses = generateSequence(id) { classId ->
            classId.outerClassId
        }.mapTo(mutableListOf()) { firProvider.getFirClassifierByFqName(it)!! }
        designation += outerClasses.asReversed()
        if (this is FirCallableDeclaration<*>) {
            designation += this
        }
    }
    if (designation.all { it.resolvePhase >= toPhase }) {
        return
    }
    val transformer = FirDesignatedBodyResolveTransformer(
        designation.iterator(), state.getSession(psi as KtElement),
        implicitTypeOnly = toPhase == FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
    )
    file.transform<FirFile, ResolutionMode>(transformer, ResolutionMode.ContextDependent)
}

fun KtElement.getOrBuildFir(
    state: FirModuleResolveState,
    phase: FirResolvePhase = FirResolvePhase.BODY_RESOLVE
): FirElement {
    val containerFir: FirDeclaration =
        when (val container = this.containingNonLocalDeclaration()) {
            is KtCallableDeclaration -> container.getOrBuildFir(state, phase)
            is KtClassOrObject -> container.getOrBuildFir(state, phase)
            null -> containingKtFile.getOrBuildFir(state, phase)
            else -> error("Unsupported: ${container.text}")
        }

    val psi = when (this) {
        is KtPropertyDelegate -> this.expression ?: this
        else -> this
    }
    return state[this] ?: run {
        containerFir.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                (element.psi as? KtElement)?.let {
                    state.record(it, element)
                }
                element.acceptChildren(this)
            }

            override fun visitReference(reference: FirReference) {}

            override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {}

            override fun visitNamedReference(namedReference: FirNamedReference) {}

            override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {}

            override fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference) {}

            override fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference) {}

            override fun visitSuperReference(superReference: FirSuperReference) {}

            override fun visitThisReference(thisReference: FirThisReference) {}

            override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {}

            override fun visitUserTypeRef(userTypeRef: FirUserTypeRef) {
                userTypeRef.acceptChildren(this)
            }
        })
        var current: PsiElement? = psi
        while (current is KtElement) {
            val mappedFir = state[current]
            if (mappedFir != null) {
                if (current != this) {
                    state.record(current, mappedFir)
                }
                return mappedFir
            }
            current = current.parent
        }
        error("FirElement is not found for: $text")
    }
}
