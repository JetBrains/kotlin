/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinDescriptorIconProvider
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.completion.handlers.BaseDeclarationInsertHandler
import org.jetbrains.kotlin.idea.completion.handlers.KotlinClassifierInsertHandler
import org.jetbrains.kotlin.idea.completion.handlers.KotlinFunctionInsertHandler
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.core.completion.PackageLookupObject
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class BasicLookupElementFactory(
        private val project: Project,
        val insertHandlerProvider: InsertHandlerProvider
) {
    companion object {
        // we skip parameter names in functional types in most of cases for shortness
        val SHORT_NAMES_RENDERER = DescriptorRenderer.SHORT_NAMES_IN_TYPES.withOptions { parameterNamesInFunctionalTypes = false }
    }

    fun createLookupElement(
            descriptor: DeclarationDescriptor,
            qualifyNestedClasses: Boolean = false,
            includeClassTypeArguments: Boolean = true,
            parametersAndTypeGrayed: Boolean = false
    ): LookupElement {
        val _descriptor = if (descriptor is CallableMemberDescriptor)
            DescriptorUtils.unwrapFakeOverride(descriptor)
        else
            descriptor
        return createLookupElementUnwrappedDescriptor(_descriptor, qualifyNestedClasses, includeClassTypeArguments, parametersAndTypeGrayed)
    }

    fun createLookupElementForJavaClass(psiClass: PsiClass, qualifyNestedClasses: Boolean = false, includeClassTypeArguments: Boolean = true): LookupElement {
        val lookupObject = object : DeclarationLookupObjectImpl(null) {
            override val psiElement: PsiElement?
                get() = psiClass
            override fun getIcon(flags: Int) = psiClass.getIcon(flags)
        }
        var element = LookupElementBuilder.create(lookupObject, psiClass.name!!)
                .withInsertHandler(KotlinClassifierInsertHandler)

        val typeParams = psiClass.typeParameters
        if (includeClassTypeArguments && typeParams.isNotEmpty()) {
            element = element.appendTailText(typeParams.map { it.name }.joinToString(", ", "<", ">"), true)
        }

        val qualifiedName = psiClass.qualifiedName!!
        var containerName = qualifiedName.substringBeforeLast('.', FqName.ROOT.toString())

        if (qualifyNestedClasses) {
            val nestLevel = psiClass.parents.takeWhile { it is PsiClass }.count()
            if (nestLevel > 0) {
                var itemText = psiClass.name
                for (i in 1..nestLevel) {
                    val outerClassName = containerName.substringAfterLast('.')
                    element = element.withLookupString(outerClassName)
                    itemText = outerClassName + "." + itemText
                    containerName = containerName.substringBeforeLast('.', FqName.ROOT.toString())
                }
                element = element.withPresentableText(itemText!!)
            }
        }

        element = element.appendTailText(" ($containerName)", true)

        if (lookupObject.isDeprecated) {
            element = element.withStrikeoutness(true)
        }

        return element.withIconFromLookupObject()
    }

    fun createLookupElementForPackage(name: FqName): LookupElement {
        var element = LookupElementBuilder.create(PackageLookupObject(name), name.shortName().asString())

        element = element.withInsertHandler(BaseDeclarationInsertHandler())

        if (!name.parent().isRoot) {
            element = element.appendTailText(" (${name.asString()})", true)
        }

        return element.withIconFromLookupObject()
    }

    private fun createLookupElementUnwrappedDescriptor(
            descriptor: DeclarationDescriptor,
            qualifyNestedClasses: Boolean,
            includeClassTypeArguments: Boolean,
            parametersAndTypeGrayed: Boolean
    ): LookupElement {
        val declarationLazy by lazy { DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor) }

        if (descriptor is JavaClassDescriptor &&
            declarationLazy is PsiClass &&
            declarationLazy !is KtLightClass) {
            // for java classes we create special lookup elements
            // because they must be equal to ones created in TypesCompletion
            // otherwise we may have duplicates
            return createLookupElementForJavaClass(declarationLazy, qualifyNestedClasses, includeClassTypeArguments)
        }

        if (descriptor is PackageViewDescriptor) {
            return createLookupElementForPackage(descriptor.fqName)
        }
        if (descriptor is PackageFragmentDescriptor) {
            return createLookupElementForPackage(descriptor.fqName)
        }

        val lookupObject: DeclarationLookupObject
        val name: String = when (descriptor) {
            is ConstructorDescriptor -> {
                // for constructor use name and icon of containing class
                val classifierDescriptor = descriptor.containingDeclaration
                lookupObject = object : DeclarationLookupObjectImpl(descriptor) {
                    override val psiElement by lazy { DescriptorToSourceUtilsIde.getAnyDeclaration(project, classifierDescriptor) }
                    override fun getIcon(flags: Int) = KotlinDescriptorIconProvider.getIcon(classifierDescriptor, psiElement, flags)
                }
                classifierDescriptor.name.asString()
            }

            is SyntheticJavaPropertyDescriptor -> {
                lookupObject = object : DeclarationLookupObjectImpl(descriptor) {
                    override val psiElement by lazy { DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor.getMethod) }
                    override fun getIcon(flags: Int) = KotlinDescriptorIconProvider.getIcon(descriptor, null, flags)
                }
                descriptor.name.asString()
            }

            else -> {
                lookupObject = object : DeclarationLookupObjectImpl(descriptor) {
                    override val psiElement by lazy { DescriptorToSourceUtils.getSourceFromDescriptor(descriptor) }

                    override fun getIcon(flags: Int) = KotlinDescriptorIconProvider.getIcon(descriptor, psiElement, flags)
                }
                descriptor.name.asString()
            }
        }

        var element = LookupElementBuilder.create(lookupObject, name)

        val insertHandler = insertHandlerProvider.insertHandler(descriptor)
        element = element.withInsertHandler(insertHandler)

        when (descriptor) {
            is FunctionDescriptor -> {
                val returnType = descriptor.returnType
                element = element.withTypeText(if (returnType != null) SHORT_NAMES_RENDERER.renderType(returnType) else "", parametersAndTypeGrayed)

                val insertsLambda = (insertHandler as? KotlinFunctionInsertHandler.Normal)?.lambdaInfo != null
                if (insertsLambda) {
                    element = element.appendTailText(" {...} ", parametersAndTypeGrayed)
                }

                element = element.appendTailText(SHORT_NAMES_RENDERER.renderFunctionParameters(descriptor), parametersAndTypeGrayed || insertsLambda)
            }

            is VariableDescriptor -> {
                element = element.withTypeText(SHORT_NAMES_RENDERER.renderType(descriptor.type), parametersAndTypeGrayed)
            }

            is ClassifierDescriptorWithTypeParameters -> {
                val typeParams = descriptor.declaredTypeParameters
                if (includeClassTypeArguments && typeParams.isNotEmpty()) {
                    element = element.appendTailText(typeParams.joinToString(", ", "<", ">") { it.name.asString() }, true)
                }

                var container = descriptor.containingDeclaration

                if (descriptor.isArtificialImportAliasedDescriptor) {
                    container = descriptor.original // we show original descriptor instead of container for import aliased descriptors
                }
                else if (qualifyNestedClasses) {
                    element = element.withPresentableText(SHORT_NAMES_RENDERER.renderClassifierName(descriptor))

                    while (container is ClassDescriptor) {
                        val containerName = container.name
                        if (!containerName.isSpecial) {
                            element = element.withLookupString(containerName.asString())
                        }
                        container = container.containingDeclaration
                    }
                }

                if (container is PackageFragmentDescriptor || container is ClassifierDescriptor) {
                    element = element.appendTailText(" (" + DescriptorUtils.getFqName(container) + ")", true)
                }

                if (descriptor is TypeAliasDescriptor) {
                    // here we render with DescriptorRenderer.SHORT_NAMES_IN_TYPES to include parameter names in functional types
                    element = element.withTypeText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(descriptor.underlyingType), false)
                }
            }

            else -> {
                element = element.withTypeText(SHORT_NAMES_RENDERER.render(descriptor), parametersAndTypeGrayed)
            }
        }

        if (descriptor is CallableDescriptor) {
            appendContainerAndReceiverInformation(descriptor) { element = element.appendTailText(it, true) }
        }

        if (descriptor is PropertyDescriptor) {
            val getterName = JvmAbi.getterName(name)
            if (getterName != name) {
                element = element.withLookupString(getterName)
            }
            if (descriptor.isVar) {
                element = element.withLookupString(JvmAbi.setterName(name))
            }
        }

        if (lookupObject.isDeprecated) {
            element = element.withStrikeoutness(true)
        }

        if ((insertHandler as? KotlinFunctionInsertHandler.Normal)?.lambdaInfo != null) {
            element.putUserData(KotlinCompletionCharFilter.ACCEPT_OPENING_BRACE, Unit)
        }

        return element.withIconFromLookupObject()
    }

    fun appendContainerAndReceiverInformation(descriptor: CallableDescriptor, appendTailText: (String) -> Unit) {
        val information = CompletionInformationProvider.EP_NAME.extensions.firstNotNullResult {
            it.getContainerAndReceiverInformation(descriptor)
        }

        if (information != null) {
            appendTailText(information)
            return
        }

        val extensionReceiver = descriptor.original.extensionReceiverParameter
        if (extensionReceiver != null) {
            when {
                descriptor is SamAdapterExtensionFunctionDescriptor -> {
                    // no need to show them as extensions
                    return
                }

                descriptor is SyntheticJavaPropertyDescriptor -> {
                    var from = descriptor.getMethod.name.asString() + "()"
                    descriptor.setMethod?.let { from += "/" + it.name.asString() + "()" }
                    appendTailText(" (from $from)")
                    return
                }

                else -> {
                    val receiverPresentation = SHORT_NAMES_RENDERER.renderType(extensionReceiver.type)
                    appendTailText(" for $receiverPresentation")
                }
            }
        }

        val containerPresentation = containerPresentation(descriptor)
        if (containerPresentation != null) {
            appendTailText(" ")
            appendTailText(containerPresentation)
        }
    }

    private fun containerPresentation(descriptor: DeclarationDescriptor): String? {
        when {
            descriptor.isArtificialImportAliasedDescriptor -> {
                return "(${DescriptorUtils.getFqName(descriptor.original)})"
            }

            descriptor.isExtension -> {
                val container = descriptor.containingDeclaration
                val containerPresentation = when (container) {
                    is ClassDescriptor -> DescriptorUtils.getFqNameFromTopLevelClass(container).toString()
                    is PackageFragmentDescriptor -> container.fqName.toString()
                    else -> return null
                }
                return "in $containerPresentation"
            }

            else -> {
                val container = descriptor.containingDeclaration as? PackageFragmentDescriptor
                                // we show container only for global functions and properties
                                ?: return null
                //TODO: it would be probably better to show it also for static declarations which are not from the current class (imported)
                return "(${container.fqName})"
            }
        }
    }

    private fun LookupElement.withIconFromLookupObject(): LookupElement {
        // add icon in renderElement only to pass presentation.isReal()
        return object : LookupElementDecorator<LookupElement>(this) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.icon = DefaultLookupItemRenderer.getRawIcon(this@withIconFromLookupObject, presentation.isReal)
            }
        }
    }
}