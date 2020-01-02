/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.fir.FirResolution
import org.jetbrains.kotlin.idea.fir.firResolveState
import org.jetbrains.kotlin.idea.fir.getOrBuildFir
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.util.*

interface KtReference : PsiPolyVariantReference {
    fun resolveToDescriptors(bindingContext: BindingContext): Collection<DeclarationDescriptor>

    override fun getElement(): KtElement

    val resolvesByNames: Collection<Name>
}

abstract class AbstractKtReference<T : KtElement>(element: T) : PsiPolyVariantReferenceBase<T>(element), KtReference {
    val expression: T
        get() = element

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        @Suppress("UNCHECKED_CAST")
        val kotlinResolver = KOTLIN_RESOLVER as ResolveCache.PolyVariantResolver<AbstractKtReference<T>>
        return ResolveCache.getInstance(expression.project).resolveWithCaching(this, kotlinResolver, false, incompleteCode)
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        return matchesTarget(element)
    }

    override fun getCanonicalText(): String = "<TBD>"

    open fun canRename(): Boolean = false
    override fun handleElementRename(newElementName: String): PsiElement? = throw IncorrectOperationException()

    override fun bindToElement(element: PsiElement): PsiElement = throw IncorrectOperationException()

    @Suppress("UNCHECKED_CAST")
    override fun getVariants(): Array<Any> = PsiReference.EMPTY_ARRAY as Array<Any>

    override fun isSoft(): Boolean = false

    override fun resolveToDescriptors(bindingContext: BindingContext): Collection<DeclarationDescriptor> {
        return getTargetDescriptors(bindingContext)
    }

    protected abstract fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor>

    override fun toString() = this::class.java.simpleName + ": " + expression.text

    companion object {
        private object FirReferenceResolveHelper {
            fun FirResolvedTypeRef.toTargetPsi(session: FirSession): PsiElement? {
                val type = type as? ConeLookupTagBasedType ?: return null
                return (type.lookupTag.toSymbol(session) as? AbstractFirBasedSymbol<*>)?.fir?.psi
            }

            fun ClassId.toTargetPsi(session: FirSession, calleeReference: FirReference? = null): PsiElement? {
                val classLikeDeclaration = ConeClassLikeLookupTagImpl(this).toSymbol(session)?.fir
                if (classLikeDeclaration is FirRegularClass) {
                    if (calleeReference is FirResolvedNamedReference) {
                        val callee = calleeReference.resolvedSymbol.fir as? FirCallableMemberDeclaration
                        // TODO: check callee owner directly?
                        if (callee !is FirConstructor && callee?.isStatic != true) {
                            classLikeDeclaration.companionObject?.let { return it.psi }
                        }
                    }
                }
                return classLikeDeclaration?.psi
            }

            fun FirReference.toTargetPsi(session: FirSession): PsiElement? {
                return when (this) {
                    is FirResolvedNamedReference -> {
                        resolvedSymbol.fir.psi
                    }
                    is FirResolvedCallableReference -> {
                        resolvedSymbol.fir.psi
                    }
                    is FirThisReference -> {
                        boundSymbol?.fir?.psi
                    }
                    is FirSuperReference -> {
                        (superTypeRef as? FirResolvedTypeRef)?.toTargetPsi(session)
                    }
                    else -> {
                        null
                    }
                }
            }

            fun resolveToPsiElements(ref: AbstractKtReference<KtElement>): Collection<PsiElement> {
                val expression = ref.expression
                val state = expression.firResolveState()
                val session = state.getSession(expression)
                when (val fir = expression.getOrBuildFir(state)) {
                    is FirResolvable -> {
                        return listOfNotNull(fir.calleeReference.toTargetPsi(session))
                    }
                    is FirResolvedTypeRef -> {
                        return listOfNotNull(fir.toTargetPsi(session))
                    }
                    is FirResolvedQualifier -> {
                        val classId = fir.classId ?: return emptyList()
                        // Distinguish A.foo() from A(.Companion).foo()
                        // Make expression.parent as? KtDotQualifiedExpression local function
                        var parent = expression.parent as? KtDotQualifiedExpression
                        while (parent != null) {
                            val selectorExpression = parent.selectorExpression ?: break
                            if (selectorExpression === expression) {
                                parent = parent.parent as? KtDotQualifiedExpression
                                continue
                            }
                            val parentFir = selectorExpression.getOrBuildFir(state)
                            if (parentFir is FirQualifiedAccess) {
                                return listOfNotNull(classId.toTargetPsi(session, parentFir.calleeReference))
                            }
                            parent = parent.parent as? KtDotQualifiedExpression
                        }
                        return listOfNotNull(classId.toTargetPsi(session))
                    }
                    is FirAnnotationCall -> {
                        val type = fir.typeRef as? FirResolvedTypeRef ?: return emptyList()
                        return listOfNotNull(type.toTargetPsi(session))
                    }
                    is FirResolvedImport -> {
                        var parent = expression.parent
                        while (parent is KtDotQualifiedExpression) {
                            if (parent.selectorExpression !== expression) {
                                // Special: package reference in the middle of import directive
                                // import a.<caret>b.c.SomeClass
                                // TODO: return reference to PsiPackage
                                return listOf(expression)
                            }
                            parent = parent.parent
                        }
                        val classId = fir.resolvedClassId
                        if (classId != null) {
                            return listOfNotNull(classId.toTargetPsi(session))
                        }
                        val name = fir.importedName ?: return emptyList()
                        val symbolProvider = session.firSymbolProvider
                        return symbolProvider.getTopLevelCallableSymbols(fir.packageFqName, name).mapNotNull { it.fir.psi } +
                                listOfNotNull(symbolProvider.getClassLikeSymbolByFqName(ClassId(fir.packageFqName, name))?.fir?.psi)
                    }
                    is FirFile -> {
                        if (expression.getNonStrictParentOfType<KtPackageDirective>() != null) {
                            // Special: package reference in the middle of package directive
                            return listOf(expression)
                        }
                        return listOfNotNull(fir.psi)
                    }
                    is FirArrayOfCall -> {
                        // We can't yet find PsiElement for arrayOf, intArrayOf, etc.
                        return emptyList()
                    }
                    is FirErrorNamedReference -> {
                        return emptyList()
                    }
                    else -> {
                        // Handle situation when we're in the middle/beginning of qualifier
                        // <caret>A.B.C.foo() or A.<caret>B.C.foo()
                        // NB: in this case we get some parent FIR, like FirBlock, FirProperty, FirFunction or the like
                        var parent = expression.parent as? KtDotQualifiedExpression
                        var unresolvedCounter = 1
                        while (parent != null) {
                            val selectorExpression = parent.selectorExpression ?: break
                            if (selectorExpression === expression) {
                                parent = parent.parent as? KtDotQualifiedExpression
                                continue
                            }
                            val parentFir = selectorExpression.getOrBuildFir(state)
                            if (parentFir is FirResolvedQualifier) {
                                var classId = parentFir.classId
                                while (unresolvedCounter > 0) {
                                    unresolvedCounter--
                                    classId = classId?.outerClassId
                                }
                                return listOfNotNull(classId?.toTargetPsi(session))
                            }
                            parent = parent.parent as? KtDotQualifiedExpression
                            unresolvedCounter++
                        }
                        return emptyList()
                    }
                }
            }
        }

        class KotlinReferenceResolver : ResolveCache.PolyVariantResolver<AbstractKtReference<KtElement>> {
            class KotlinResolveResult(element: PsiElement) : PsiElementResolveResult(element)

            private fun resolveToPsiElements(ref: AbstractKtReference<KtElement>): Collection<PsiElement> {
                if (FirResolution.enabled) {
                    return FirReferenceResolveHelper.resolveToPsiElements(ref)
                }
                val bindingContext = ref.expression.analyze(BodyResolveMode.PARTIAL)
                return resolveToPsiElements(ref, bindingContext, ref.getTargetDescriptors(bindingContext))
            }

            private fun resolveToPsiElements(
                ref: AbstractKtReference<KtElement>,
                context: BindingContext,
                targetDescriptors: Collection<DeclarationDescriptor>
            ): Collection<PsiElement> {
                if (targetDescriptors.isNotEmpty()) {
                    return targetDescriptors.flatMap { target -> resolveToPsiElements(ref, target) }.toSet()
                }

                val labelTargets = getLabelTargets(ref, context)
                if (labelTargets != null) {
                    return labelTargets
                }

                return Collections.emptySet()
            }

            private fun resolveToPsiElements(
                ref: AbstractKtReference<KtElement>,
                targetDescriptor: DeclarationDescriptor
            ): Collection<PsiElement> {
                return if (targetDescriptor is PackageViewDescriptor) {
                    val psiFacade = JavaPsiFacade.getInstance(ref.expression.project)
                    val fqName = targetDescriptor.fqName.asString()
                    listOfNotNull(psiFacade.findPackage(fqName))
                } else {
                    DescriptorToSourceUtilsIde.getAllDeclarations(ref.expression.project, targetDescriptor, ref.expression.resolveScope)
                }
            }

            private fun getLabelTargets(ref: AbstractKtReference<KtElement>, context: BindingContext): Collection<PsiElement>? {
                val reference = ref.expression as? KtReferenceExpression ?: return null
                val labelTarget = context[BindingContext.LABEL_TARGET, reference]
                if (labelTarget != null) {
                    return listOf(labelTarget)
                }
                return context[BindingContext.AMBIGUOUS_LABEL_TARGET, reference]
            }

            override fun resolve(ref: AbstractKtReference<KtElement>, incompleteCode: Boolean): Array<ResolveResult> {
                val resolveToPsiElements = resolveToPsiElements(ref)
                return resolveToPsiElements.map { KotlinResolveResult(it) }.toTypedArray()
            }
        }

        val KOTLIN_RESOLVER = KotlinReferenceResolver()
    }
}

abstract class KtSimpleReference<T : KtReferenceExpression>(expression: T) : AbstractKtReference<T>(expression) {
    override fun getTargetDescriptors(context: BindingContext) = expression.getReferenceTargets(context)
}

abstract class KtMultiReference<T : KtElement>(expression: T) : AbstractKtReference<T>(expression)
