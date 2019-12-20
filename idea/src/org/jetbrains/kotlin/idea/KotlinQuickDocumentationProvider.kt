/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.google.common.html.HtmlEscapers
import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.codeInsight.javadoc.JavaDocInfoGeneratorFactory
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.lang.java.JavaDocumentationProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.DocumentationURLs.LATE_INITIALIZED_PROPERTIES_AND_VARIABLES_URL
import org.jetbrains.kotlin.idea.DocumentationURLs.TAIL_RECURSIVE_FUNCTIONS_URL
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.decompiler.navigation.SourceNavigationHelper
import org.jetbrains.kotlin.idea.kdoc.*
import org.jetbrains.kotlin.idea.kdoc.KDocRenderer.appendKDocContent
import org.jetbrains.kotlin.idea.kdoc.KDocRenderer.appendKDocSection
import org.jetbrains.kotlin.idea.kdoc.KDocTemplate.DescriptionBodyTemplate
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.util.isRunningInCidrIde
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.AnnotationArgumentsRenderingPolicy
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.deprecation.deprecatedByAnnotationReplaceWithExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.constant
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private object DocumentationURLs {
    const val LATE_INITIALIZED_PROPERTIES_AND_VARIABLES_URL =
        "https://kotlinlang.org/docs/reference/properties.html#late-initialized-properties-and-variables"

    const val TAIL_RECURSIVE_FUNCTIONS_URL =
        "https://kotlinlang.org/docs/reference/functions.html#tail-recursive-functions"
}

class HtmlClassifierNamePolicy(val base: ClassifierNamePolicy) : ClassifierNamePolicy {
    override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
        if (DescriptorUtils.isAnonymousObject(classifier)) {

            val supertypes = classifier.typeConstructor.supertypes
            return buildString {
                append("&lt;anonymous object")
                if (supertypes.isNotEmpty()) {
                    append(" : ")
                    supertypes.joinTo(this) {
                        val ref = it.constructor.declarationDescriptor
                        if (ref != null)
                            renderClassifier(ref, renderer)
                        else
                            "&lt;ERROR CLASS&gt;"
                    }
                }
                append("&gt;")
            }
        }

        val name = base.renderClassifier(classifier, renderer)
        if (classifier.isBoringBuiltinClass())
            return name
        return buildString {
            val ref = classifier.fqNameUnsafe.toString()
            DocumentationManagerUtil.createHyperlink(this, ref, name, true)
        }
    }
}

class WrapValueParameterHandler(val base: DescriptorRenderer.ValueParametersHandler) : DescriptorRenderer.ValueParametersHandler {


    override fun appendBeforeValueParameters(parameterCount: Int, builder: StringBuilder) {
        base.appendBeforeValueParameters(parameterCount, builder)
    }

    override fun appendBeforeValueParameter(
        parameter: ValueParameterDescriptor,
        parameterIndex: Int,
        parameterCount: Int,
        builder: StringBuilder
    ) {
        builder.append("\n    ")
        base.appendBeforeValueParameter(parameter, parameterIndex, parameterCount, builder)
    }

    override fun appendAfterValueParameter(
        parameter: ValueParameterDescriptor,
        parameterIndex: Int,
        parameterCount: Int,
        builder: StringBuilder
    ) {
        if (parameterIndex != parameterCount - 1) {
            builder.append(",")
        }
    }

    override fun appendAfterValueParameters(parameterCount: Int, builder: StringBuilder) {
        if (parameterCount > 0) {
            builder.appendln()
        }
        base.appendAfterValueParameters(parameterCount, builder)
    }

}


class KotlinQuickDocumentationProvider : AbstractDocumentationProvider() {

    override fun getCustomDocumentationElement(editor: Editor, fil: PsiFile, contextElement: PsiElement?): PsiElement? {
        return if (contextElement.isModifier()) contextElement else null
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        return if (element == null) null else getText(element, originalElement, true)
    }

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        return getText(element, originalElement, false)
    }

    override fun getDocumentationElementForLink(psiManager: PsiManager, link: String, context: PsiElement?): PsiElement? {
        val navElement = context?.navigationElement as? KtElement ?: return null
        val bindingContext = navElement.analyze(BodyResolveMode.PARTIAL)
        val contextDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, navElement] ?: return null
        val descriptors = resolveKDocLink(
            bindingContext, navElement.getResolutionFacade(),
            contextDescriptor, null, link.split('.')
        )
        val target = descriptors.firstOrNull() ?: return null
        return DescriptorToSourceUtilsIde.getAnyDeclaration(psiManager.project, target)
    }

    override fun getDocumentationElementForLookupItem(psiManager: PsiManager, `object`: Any?, element: PsiElement?): PsiElement? {
        if (`object` is DeclarationLookupObject) {
            `object`.psiElement?.let { return it }
            `object`.descriptor?.let { descriptor ->
                return DescriptorToSourceUtilsIde.getAnyDeclaration(psiManager.project, descriptor)
            }
        }
        return null
    }

    companion object {
        private val LOG = Logger.getInstance(KotlinQuickDocumentationProvider::class.java)

        private val DESCRIPTOR_RENDERER = DescriptorRenderer.HTML.withOptions {
            classifierNamePolicy = HtmlClassifierNamePolicy(ClassifierNamePolicy.SHORT)
            valueParametersHandler = WrapValueParameterHandler(valueParametersHandler)
            annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
            renderCompanionObjectName = true
            withDefinedIn = false
            eachAnnotationOnNewLine = true
            boldOnlyForNamesInHtml = true
        }

        private fun renderEnumSpecialFunction(element: KtClass, functionDescriptor: FunctionDescriptor, quickNavigation: Boolean): String {
            val kdoc = run {
                val declarationDescriptor = element.resolveToDescriptorIfAny()
                val enumDescriptor = declarationDescriptor?.getSuperClassNotAny() ?: return@run null

                val enumDeclaration =
                    DescriptorToSourceUtilsIde.getAnyDeclaration(element.project, enumDescriptor) as? KtDeclaration ?: return@run null

                val enumSource = SourceNavigationHelper.getNavigationElement(enumDeclaration)
                val functionName = functionDescriptor.fqNameSafe.shortName().asString()
                return@run enumSource.findDescendantOfType<KDoc> {
                    it.getChildrenOfType<KDocSection>().any { it.findTagByName(functionName) != null }
                }
            }

            val section = kdoc?.getDefaultSection()

            return buildString {
                insert(KDocTemplate()) {
                    definition {
                        renderDefinition(functionDescriptor, DESCRIPTOR_RENDERER)
                    }
                    if (!quickNavigation && section != null) {
                        description {
                            insert(DescriptionBodyTemplate.Kotlin()) {
                                content {
                                    appendKDocContent(section)
                                }
                                sections {
                                    appendKDocSection(section)
                                }
                            }
                        }
                    }
                }
            }
        }


        private fun renderEnum(element: KtClass, originalElement: PsiElement?, quickNavigation: Boolean): String? {
            val referenceExpression = originalElement?.getNonStrictParentOfType<KtReferenceExpression>()
            if (referenceExpression != null) {
                // When caret on special enum function (e.g SomeEnum.values<caret>())
                // element is not an KtReferenceExpression, but KtClass of enum
                // so reference extracted from originalElement
                val context = referenceExpression.analyze(BodyResolveMode.PARTIAL)
                (context[BindingContext.REFERENCE_TARGET, referenceExpression]
                    ?: context[BindingContext.REFERENCE_TARGET, referenceExpression.getChildOfType<KtReferenceExpression>()])?.let {
                    if (it is FunctionDescriptor) // To protect from Some<caret>Enum.values()
                        return renderEnumSpecialFunction(element, it, quickNavigation)
                }
            }
            return renderKotlinDeclaration(element, quickNavigation)
        }

        private fun getText(element: PsiElement, originalElement: PsiElement?, quickNavigation: Boolean) =
            getTextImpl(element, originalElement, quickNavigation)

        private fun getTextImpl(element: PsiElement, originalElement: PsiElement?, quickNavigation: Boolean): String? {
            if (element is PsiWhiteSpace) {
                val itElement = findElementWithText(originalElement, "it")
                val itReference = itElement?.getParentOfType<KtNameReferenceExpression>(false)
                if (itReference != null) {
                    return getTextImpl(itReference, originalElement, quickNavigation)
                }
            }

            if (element is KtTypeReference) {
                val declaration = element.parent
                if (declaration is KtCallableDeclaration && declaration.receiverTypeReference == element) {
                    val thisElement = findElementWithText(originalElement, "this")
                    if (thisElement != null) {
                        return getTextImpl(declaration, originalElement, quickNavigation)
                    }
                }
            }

            if (element is KtClass && element.isEnum()) {
                // When caret on special enum function (e.g SomeEnum.values<caret>())
                // element is not an KtReferenceExpression, but KtClass of enum
                return renderEnum(element, originalElement, quickNavigation)
            } else if (element is KtEnumEntry && !quickNavigation) {
                val ordinal = element.containingClassOrObject?.getBody()?.run { getChildrenOfType<KtEnumEntry>().indexOf(element) }

                return buildString {
                    insert(buildKotlinDeclaration(element, quickNavigation)) {
                        definition {
                            it.inherit()
                            ordinal?.let {
                                append("<br>Enum constant ordinal: $ordinal")
                            }
                        }
                    }
                }
            } else if (element is KtDeclaration) {
                return renderKotlinDeclaration(element, quickNavigation)
            } else if (element is KtNameReferenceExpression && element.getReferencedName() == "it") {
                return renderKotlinImplicitLambdaParameter(element, quickNavigation)
            } else if (element is KtLightDeclaration<*, *>) {
                val origin = element.kotlinOrigin ?: return null
                return renderKotlinDeclaration(origin, quickNavigation)
            } else if (element.isModifier()) {
                when (element.text) {
                    KtTokens.LATEINIT_KEYWORD.value -> {
                        return "lateinit allows initializing a ${a(
                            LATE_INITIALIZED_PROPERTIES_AND_VARIABLES_URL,
                            "non-null property outside of a constructor"
                        )}"
                    }

                    KtTokens.TAILREC_KEYWORD.value -> {
                        return "tailrec marks a function as ${a(TAIL_RECURSIVE_FUNCTIONS_URL, "tail-recursive")} " +
                                "(allowing the compiler to replace recursion with iteration)"
                    }
                }
            }

            if (quickNavigation) {
                val referenceExpression = originalElement?.getNonStrictParentOfType<KtReferenceExpression>()
                if (referenceExpression != null) {
                    val context = referenceExpression.analyze(BodyResolveMode.PARTIAL)
                    val declarationDescriptor = context[BindingContext.REFERENCE_TARGET, referenceExpression]
                    if (declarationDescriptor != null) {
                        return mixKotlinToJava(declarationDescriptor, element, originalElement)
                    }
                }
            } else {
                // This element was resolved to non-kotlin element, it will be rendered with own provider
            }

            return null
        }

        private fun renderKotlinDeclaration(declaration: KtExpression, quickNavigation: Boolean) = buildString {
            insert(buildKotlinDeclaration(declaration, quickNavigation)) {}
        }

        private fun buildKotlinDeclaration(declaration: KtExpression, quickNavigation: Boolean): KDocTemplate {
            val context = declaration.analyze(BodyResolveMode.PARTIAL)
            val declarationDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]

            if (declarationDescriptor == null) {
                LOG.info("Failed to find descriptor for declaration " + declaration.getElementTextWithContext())
                return KDocTemplate.NoDocTemplate().apply {
                    error {
                        append("No documentation available")
                    }
                }
            }

            return buildKotlin(context, declarationDescriptor, quickNavigation, declaration)
        }

        private fun renderKotlinImplicitLambdaParameter(element: KtReferenceExpression, quickNavigation: Boolean): String? {
            val context = element.analyze(BodyResolveMode.PARTIAL)
            val target = element.mainReference.resolveToDescriptors(context).singleOrNull() as? ValueParameterDescriptor? ?: return null
            return renderKotlin(context, target, quickNavigation, element)
        }

        private fun renderKotlin(
            context: BindingContext,
            declarationDescriptor: DeclarationDescriptor,
            quickNavigation: Boolean,
            ktElement: KtElement
        ) = buildString {
            insert(buildKotlin(context, declarationDescriptor, quickNavigation, ktElement)) {}
        }

        private fun buildKotlin(
            context: BindingContext,
            declarationDescriptor: DeclarationDescriptor,
            quickNavigation: Boolean,
            ktElement: KtElement
        ): KDocTemplate {
            @Suppress("NAME_SHADOWING")
            var declarationDescriptor = declarationDescriptor
            if (declarationDescriptor is ValueParameterDescriptor) {
                val property = context[BindingContext.VALUE_PARAMETER_AS_PROPERTY, declarationDescriptor]
                if (property != null) {
                    declarationDescriptor = property
                }
            }

            val deprecationProvider = ktElement.getResolutionFacade().frontendService<DeprecationResolver>()

            return KDocTemplate().apply {
                definition {
                    renderDefinition(declarationDescriptor, DESCRIPTOR_RENDERER)
                }

                insertDeprecationInfo(declarationDescriptor, deprecationProvider)

                if (!quickNavigation) {
                    description {
                        val comment = declarationDescriptor.findKDoc { DescriptorToSourceUtilsIde.getAnyDeclaration(ktElement.project, it) }
                        if (comment != null) {
                            insert(DescriptionBodyTemplate.Kotlin()) {
                                content {
                                    appendKDocContent(comment)
                                }
                                sections {
                                    if (comment is KDocSection) appendKDocSection(comment)
                                }
                            }
                        } else if (declarationDescriptor is CallableDescriptor) { // If we couldn't find KDoc, try to find javadoc in one of super's
                            insert(DescriptionBodyTemplate.FromJava()) {
                                body = extractJavaDescription(declarationDescriptor)
                            }
                        }
                    }
                }
            }
        }

        private fun StringBuilder.renderDefinition(descriptor: DeclarationDescriptor, renderer: DescriptorRenderer) {
            if (!DescriptorUtils.isLocal(descriptor)) {
                val containingDeclaration = descriptor.containingDeclaration
                if (containingDeclaration != null) {
                    val fqName = containingDeclaration.fqNameSafe
                    if (!fqName.isRoot) {
                        DocumentationManagerUtil.createHyperlink(this, fqName.asString(), fqName.asString(), false)
                    }
                    val fileName =
                        descriptor
                            .safeAs<DeclarationDescriptorWithSource>()
                            ?.source
                            ?.containingFile
                            ?.name
                            ?.takeIf { containingDeclaration is PackageFragmentDescriptor }

                    if (fileName != null) {
                        if (!fqName.isRoot) {
                            append(" ")
                        }
                        wrap("<font color=\"808080\"><i>", "</i></font>") {
                            append(fileName)
                        }
                    }
                    if (fileName != null || !fqName.isRoot) {
                        append("<br>")
                    }
                }
            }

            append(renderer.render(descriptor))
        }

        private fun extractJavaDescription(declarationDescriptor: DeclarationDescriptor): String {
            val psi = declarationDescriptor.findPsi() as? KtFunction ?: return ""
            val lightElement =
                LightClassUtil.getLightClassMethod(psi) // Light method for super's scan in javadoc info gen
            val javaDocInfoGenerator = JavaDocInfoGeneratorFactory.create(psi.project, lightElement)
            val builder = StringBuilder()
            if (javaDocInfoGenerator.generateDocInfoCore(builder, false)) {
                val renderedJava = builder.toString()
                return renderedJava.removeRange(
                    renderedJava.indexOf(DEFINITION_START),
                    renderedJava.indexOf(DEFINITION_END)
                ) // Cut off light method signature
            }
            return ""
        }

        private fun KDocTemplate.insertDeprecationInfo(
            declarationDescriptor: DeclarationDescriptor,
            deprecationResolver: DeprecationResolver
        ) {
            val deprecationInfo = deprecationResolver.getDeprecations(declarationDescriptor).firstOrNull() ?: return

            deprecation {
                deprecationInfo.message?.let { message ->
                    append(SECTION_HEADER_START)
                    append("Deprecated:")
                    append(SECTION_SEPARATOR)
                    append(message.htmlEscape())
                    append(SECTION_END)
                }
                deprecationInfo.deprecatedByAnnotationReplaceWithExpression()?.let { replaceWith ->
                    append(SECTION_HEADER_START)
                    append("Replace with:")
                    append(SECTION_SEPARATOR)
                    wrapTag("code") { append(replaceWith.htmlEscape()) }
                    append(SECTION_END)
                }
            }
        }

        private fun String.htmlEscape(): String = HtmlEscapers.htmlEscaper().escape(this)

        private inline fun StringBuilder.wrap(prefix: String, postfix: String, crossinline body: () -> Unit) {
            this.append(prefix)
            body()
            this.append(postfix)
        }

        private inline fun StringBuilder.wrapTag(tag: String, crossinline body: () -> Unit) {
            wrap("<$tag>", "</$tag>", body)
        }

        private fun a(url: String, text: String): String {
            return """<a href="$url">$text</a>"""
        }

        private fun mixKotlinToJava(
            declarationDescriptor: DeclarationDescriptor,
            element: PsiElement,
            originalElement: PsiElement?
        ): String? {
            if (isRunningInCidrIde) return null // no Java support in CIDR

            val originalInfo = JavaDocumentationProvider().getQuickNavigateInfo(element, originalElement)
            if (originalInfo != null) {
                val renderedDecl = constant { DESCRIPTOR_RENDERER.withOptions { withDefinedIn = false } }.render(declarationDescriptor)
                return "$renderedDecl<br/>Java declaration:<br/>$originalInfo"
            }

            return null
        }

        private fun findElementWithText(element: PsiElement?, text: String): PsiElement? {
            return when {
                element == null -> null
                element.text == text -> element
                element.prevLeaf()?.text == text -> element.prevLeaf()
                else -> null
            }
        }

        private fun PsiElement?.isModifier() =
            this != null && parent is KtModifierList && KtTokens.MODIFIER_KEYWORDS_ARRAY.firstOrNull { it.value == text } != null

    }
}
