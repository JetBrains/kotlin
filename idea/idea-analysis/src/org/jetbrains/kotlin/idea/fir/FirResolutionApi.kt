/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDesignatedBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.runResolve
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.selfImportingScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
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
    val memberScope =
        if (klassFqName == null) selfImportingScope(this.packageFqName, session)
        else provider.getClassDeclaredMemberScope(ClassId(packageFqName, klassFqName, false))!!
    var result: FirCallableDeclaration<*>? = null
    val processor = { symbol: FirCallableSymbol<*> ->
        val fir = symbol.fir
        if (fir.psi == callableMember) {
            result = fir
            ProcessorAction.STOP
        } else {
            ProcessorAction.NEXT
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

// NB: at this moment it crashes with ISE when called on local declaration
fun KtCallableDeclaration.getOrBuildFir(
    state: FirResolveState,
    phase: FirResolvePhase = FirResolvePhase.DECLARATIONS
): FirCallableDeclaration<*> {
    val session = state.getSession(this)

    val file = this.containingKtFile
    val packageFqName = file.packageFqName
    val klassFqName = this.containingClassOrObject?.relativeFqName()
    val declName = this.nameAsSafeName

    val firProvider = FirProvider.getInstance(session) as IdeFirProvider
    val firFile = firProvider.getOrBuildFile(file)
    val memberSymbol = firFile.findCallableMember(firProvider, this, packageFqName, klassFqName, declName).symbol
    memberSymbol.fir.runResolve(firFile, firProvider, phase, state)
    return memberSymbol.fir
}

fun KtClassOrObject.getOrBuildFir(
    state: FirResolveState,
    phase: FirResolvePhase = FirResolvePhase.DECLARATIONS
): FirRegularClass {
    val session = state.getSession(this)

    val file = this.containingKtFile
    val packageFqName = file.packageFqName
    val klassFqName = this.relativeFqName()

    val firProvider = FirProvider.getInstance(session) as IdeFirProvider
    val firFile = firProvider.getOrBuildFile(file)
    val firClass = firProvider.getFirClassifierByFqName(ClassId(packageFqName, klassFqName, false)) as FirRegularClass
    firClass.runResolve(firFile, firProvider, phase, state)
    return firClass
}

fun KtFile.getOrBuildFir(
    state: FirResolveState,
    phase: FirResolvePhase = FirResolvePhase.DECLARATIONS
): FirFile {
    val session = state.getSession(this)
    val firProvider = FirProvider.getInstance(session) as IdeFirProvider
    val firFile = firProvider.getOrBuildFile(this)
    firFile.runResolve(firFile, firProvider, phase, state)
    return firFile
}

private fun FirDeclaration.runResolve(
    file: FirFile,
    firProvider: IdeFirProvider,
    toPhase: FirResolvePhase,
    state: FirResolveState
) {
    val nonLazyPhase = minOf(toPhase, FirResolvePhase.DECLARATIONS)
    file.runResolve(toPhase = nonLazyPhase, fromPhase = this.resolvePhase)
    if (toPhase > nonLazyPhase) {
        val designation = mutableListOf<FirElement>()
        designation += file
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
        val transformer = FirDesignatedBodyResolveTransformer(
            designation.iterator(), state.getSession(psi as KtElement),
            implicitTypeOnly = toPhase == FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
        )
        file.transform<FirFile, ResolutionMode>(transformer, ResolutionMode.ContextDependent)
    }
}

fun KtElement.getOrBuildFir(
    state: FirResolveState,
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
