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
import org.jetbrains.kotlin.idea.core.completion.PackageLookupObject
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

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
        val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, _descriptor)
        return createLookupElement(_descriptor, declaration, qualifyNestedClasses, includeClassTypeArguments, parametersAndTypeGrayed)
    }

    fun createLookupElementForJavaClass(psiClass: PsiClass, qualifyNestedClasses: Boolean = false, includeClassTypeArguments: Boolean = true): LookupElement {
        val lookupObject = object : DeclarationLookupObjectImpl(null, psiClass) {
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

    private fun createLookupElement(
            descriptor: DeclarationDescriptor,
            declaration: PsiElement?,
            qualifyNestedClasses: Boolean,
            includeClassTypeArguments: Boolean,
            parametersAndTypeGrayed: Boolean
    ): LookupElement {
        if (descriptor is ClassifierDescriptor &&
            declaration is PsiClass &&
            declaration !is KtLightClass) {
            // for java classes we create special lookup elements
            // because they must be equal to ones created in TypesCompletion
            // otherwise we may have duplicates
            return createLookupElementForJavaClass(declaration, qualifyNestedClasses, includeClassTypeArguments)
        }

        if (descriptor is PackageViewDescriptor) {
            return createLookupElementForPackage(descriptor.fqName)
        }
        if (descriptor is PackageFragmentDescriptor) {
            return createLookupElementForPackage(descriptor.fqName)
        }

        // for constructor use name and icon of containing class
        val nameAndIconDescriptor: DeclarationDescriptor
        val iconDeclaration: PsiElement?
        if (descriptor is ConstructorDescriptor) {
            nameAndIconDescriptor = descriptor.containingDeclaration
            iconDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, nameAndIconDescriptor)
        }
        else {
            nameAndIconDescriptor = descriptor
            iconDeclaration = declaration
        }
        val name = nameAndIconDescriptor.name.asString()

        val psiElement = declaration
                         ?: (descriptor as? SyntheticJavaPropertyDescriptor)
                                 ?.getMethod
                                 ?.let { DescriptorToSourceUtilsIde.getAnyDeclaration(project, it) }
        val lookupObject = object : DeclarationLookupObjectImpl(descriptor, psiElement) {
            override fun getIcon(flags: Int) = KotlinDescriptorIconProvider.getIcon(nameAndIconDescriptor, iconDeclaration, flags)
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
                    element = element.appendTailText(typeParams.map { it.name.asString() }.joinToString(", ", "<", ">"), true)
                }

                var container = descriptor.containingDeclaration

                if (qualifyNestedClasses) {
                    element = element.withPresentableText(SHORT_NAMES_RENDERER.renderClassifierName(descriptor))

                    while (container is ClassDescriptor) {
                        val containerName = container.name
                        if (!containerName.isSpecial) {
                            element = element.withLookupString(containerName.asString())
                        }
                        container = container.containingDeclaration
                    }
                }

                if (container is PackageFragmentDescriptor || container is ClassDescriptor) {
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
        val extensionReceiver = descriptor.original.extensionReceiverParameter
        when {
            descriptor is SyntheticJavaPropertyDescriptor -> {
                var from = descriptor.getMethod.name.asString() + "()"
                descriptor.setMethod?.let { from += "/" + it.name.asString() + "()" }
                appendTailText(" (from $from)")
            }

        // no need to show them as extensions
            descriptor is SamAdapterExtensionFunctionDescriptor -> {
            }

            extensionReceiver != null -> {
                val receiverPresentation = SHORT_NAMES_RENDERER.renderType(extensionReceiver.type)
                appendTailText(" for $receiverPresentation")

                val container = descriptor.containingDeclaration
                val containerPresentation = if (container is ClassDescriptor)
                    DescriptorUtils.getFqNameFromTopLevelClass(container).toString()
                else if (container is PackageFragmentDescriptor)
                    container.fqName.toString()
                else
                    null
                if (containerPresentation != null) {
                    appendTailText(" in $containerPresentation")
                }
            }

            else -> {
                val container = descriptor.containingDeclaration
                if (container is PackageFragmentDescriptor) {
                    // we show container only for global functions and properties
                    //TODO: it would be probably better to show it also for static declarations which are not from the current class (imported)
                    appendTailText(" (${container.fqName})")
                }
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