/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaKDocProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.api.KtDocCommentDescriptor
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.kdoc.psi.impl.KtDocCommentDescriptorImpl
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

@KaImplementationDetail
@OptIn(KtNonPublicApi::class, KtImplementationDetail::class)
abstract class KaBaseKDocProvider<T : KaSession> : KaBaseSessionComponent<T>(), KaKDocProvider {
    override fun KtDeclaration.findKDoc(): KtDocCommentDescriptor? = this.lookupOwnedKDoc() ?: this.lookupKDocInParent()

    override fun KaDeclarationSymbol.findKDoc(): KtDocCommentDescriptor? = with(analysisSession) {
        val ktElement = psi?.navigationElement as? KtDeclaration
        ktElement?.findKDoc()?.let { return it }

        if (this@findKDoc is KaCallableSymbol) {
            allOverriddenSymbols.forEach { overrider ->
                overrider.findKDoc()?.let {
                    return it
                }
            }
        }

        if (this@findKDoc is KaValueParameterSymbol) {
            val containingSymbol = containingDeclaration as? KaNamedFunctionSymbol
            if (containingSymbol != null) {
                val idx = containingSymbol.valueParameters.indexOf(this@findKDoc)
                containingSymbol.getExpectsForActual().filterIsInstance<KaNamedFunctionSymbol>()
                    .firstNotNullOfOrNull { expectFunction ->
                        expectFunction.valueParameters[idx].findKDoc()
                    }?.let { return it }
            }
        }

        getExpectsForActual().firstNotNullOfOrNull { expectSymbol ->
            expectSymbol.findKDoc()?.let {
                return it
            }
        }

        return null
    }

    private fun KtElement.lookupOwnedKDoc(): KtDocCommentDescriptor? {
        // KDoc for the primary constructor is located inside its class KDoc
        val psiDeclaration = when (this) {
            is KtPrimaryConstructor -> getContainingClassOrObject()
            else -> this
        }

        if (psiDeclaration is KtDeclaration) {
            val kdoc = psiDeclaration.docComment
            if (kdoc != null) {
                if (this is KtConstructor<*>) {
                    // ConstructorDescriptor resolves to the same JetDeclaration
                    val constructorSection = kdoc.findSectionByTag(KDocKnownTag.CONSTRUCTOR)
                    if (constructorSection != null) {
                        // if annotated with @constructor tag and the caret is on constructor definition,
                        // then show @constructor description as the main content, and additional sections
                        // that contain @param tags (if any), as the most relatable ones
                        // practical example: val foo = Fo<caret>o("argument") -- show @constructor and @param content
                        val paramSections = kdoc.findSectionsContainingTag(KDocKnownTag.PARAM)
                        return KtDocCommentDescriptorImpl(constructorSection, paramSections)
                    }
                }
                return KtDocCommentDescriptorImpl(kdoc.getDefaultSection(), kdoc.getAllSections())
            }
        }
        return null
    }

    /**
     * Looks for sections that have a deeply nested [tag],
     * as opposed to [KDoc.findSectionByTag], which only looks among the top level
     */
    private fun KDoc.findSectionsContainingTag(tag: KDocKnownTag): List<KDocSection> {
        return getChildrenOfType<KDocSection>()
            .filter { it.findTagByName(tag.name.toLowerCaseAsciiOnly()) != null }
    }

    private fun KtDeclaration.lookupKDocInParent(): KtDocCommentDescriptor? {
        val subjectName = name
        val containingDeclaration = PsiTreeUtil.findFirstParent(this, true) {
            (it is KtDeclarationWithBody && it !is KtPrimaryConstructor) || it is KtClassOrObject
        }

        val containerKDoc = containingDeclaration?.getChildOfType<KDoc>()
        if (containerKDoc == null || subjectName == null) return null

        val propertySection = containerKDoc.findSectionByTag(KDocKnownTag.PROPERTY, subjectName)
        val paramTag = containerKDoc.findDescendantOfType<KDocTag> {
            it.knownTag == KDocKnownTag.PARAM && it.getSubjectName() == subjectName
        }

        val primaryContent = when (this) {
            // class Foo(val s: String)
            is KtParameter if this.isPropertyParameter() -> propertySection ?: paramTag
            // fun some(f: String) || class Some<T: Base> || Foo(s = "argument")
            is KtParameter, is KtTypeParameter -> paramTag
            // if this property is declared separately (outside primary constructor), but it's for some reason
            // annotated as @property in the class's description, instead of having its own KDoc
            is KtProperty if containingDeclaration is KtClassOrObject -> propertySection
            else -> null
        }

        return primaryContent?.let {
            // makes little sense to include any other sections, since we found
            // documentation for a very specific element, like a property/param
            KtDocCommentDescriptorImpl(it, additionalSections = emptyList())
        }
    }
}
