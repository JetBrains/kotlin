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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.*
import com.intellij.codeInsight.lookup.impl.LookupCellRenderer
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.asJava.KotlinLightClass
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.JetDescriptorIconProvider
import org.jetbrains.kotlin.idea.completion.handlers.*
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.fuzzyReturnType
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import java.util.*

public data class PackageLookupObject(val fqName: FqName) : DeclarationLookupObject {
    override val psiElement: PsiElement? get() = null

    override val descriptor: DeclarationDescriptor? get() = null

    override val name: Name get() = fqName.shortName()

    override val importableFqName: FqName get() = fqName

    override val isDeprecated: Boolean get() = false

    override fun getIcon(flags: Int) = PlatformIcons.PACKAGE_ICON
}

public class LookupElementFactory(
        private val resolutionFacade: ResolutionFacade,
        private val receiverTypes: Collection<JetType>,
        expectedInfosCalculator: () -> Collection<ExpectedInfo>
) {
    private val expectedInfos by lazy { expectedInfosCalculator() }

    public fun createLookupElement(
            descriptor: DeclarationDescriptor,
            boldImmediateMembers: Boolean,
            qualifyNestedClasses: Boolean = false,
            includeClassTypeArguments: Boolean = true
    ): LookupElement {
        val _descriptor = if (descriptor is CallableMemberDescriptor)
            DescriptorUtils.unwrapFakeOverride(descriptor)
        else
            descriptor
        var element = createLookupElement(_descriptor, DescriptorToSourceUtils.descriptorToDeclaration(_descriptor), qualifyNestedClasses, includeClassTypeArguments)

        val weight = callableWeight(descriptor)
        if (weight != null) {
            element.putUserData(CALLABLE_WEIGHT_KEY, weight) // store for use in lookup elements sorting
        }

        if (boldImmediateMembers) {
            element = element.boldIfImmediate(weight)
        }
        return element
    }

    private fun LookupElement.boldIfImmediate(weight: CallableWeight?): LookupElement {
        val style = when (weight) {
            CallableWeight.thisClassMember, CallableWeight.thisTypeExtension -> Style.BOLD
            CallableWeight.receiverCastRequired -> Style.GRAYED
            else -> Style.NORMAL
        }
        return if (style != Style.NORMAL) {
            object : LookupElementDecorator<LookupElement>(this) {
                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    if (style == Style.BOLD) {
                        presentation.setItemTextBold(true)
                    }
                    else {
                        presentation.setItemTextForeground(LookupCellRenderer.getGrayedForeground(false))
                        // gray all tail fragments too:
                        val fragments = presentation.getTailFragments()
                        presentation.clearTail()
                        for (fragment in fragments) {
                            presentation.appendTailText(fragment.text, true)
                        }
                    }
                }
            }
        }
        else {
            this
        }
    }

    private enum class Style {
        NORMAL,
        BOLD,
        GRAYED
    }

    public fun createLookupElementForJavaClass(psiClass: PsiClass, qualifyNestedClasses: Boolean = false, includeClassTypeArguments: Boolean = true): LookupElement {
        val lookupObject = object : DeclarationLookupObjectImpl(null, psiClass, resolutionFacade) {
            override fun getIcon(flags: Int) = psiClass.getIcon(flags)
        }
        var element = LookupElementBuilder.create(lookupObject, psiClass.getName()!!)
                .withInsertHandler(KotlinClassifierInsertHandler)

        val typeParams = psiClass.getTypeParameters()
        if (includeClassTypeArguments && typeParams.isNotEmpty()) {
            element = element.appendTailText(typeParams.map { it.getName() }.joinToString(", ", "<", ">"), true)
        }

        val qualifiedName = psiClass.getQualifiedName()!!
        var containerName = qualifiedName.substringBeforeLast('.', FqName.ROOT.toString())

        if (qualifyNestedClasses) {
            val nestLevel = psiClass.parents.takeWhile { it is PsiClass }.count()
            if (nestLevel > 0) {
                var itemText = psiClass.getName()
                for (i in 1..nestLevel) {
                    itemText = containerName.substringAfterLast('.') + "." + itemText
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

    public fun createLookupElementForPackage(name: FqName): LookupElement {
        var element = LookupElementBuilder.create(PackageLookupObject(name), name.shortName().asString())

        element = element.withInsertHandler(BaseDeclarationInsertHandler())

        if (!name.parent().isRoot()) {
            element = element.appendTailText(" (${name.asString()})", true)
        }

        return element.withIconFromLookupObject()
    }

    private fun createLookupElement(
            descriptor: DeclarationDescriptor,
            declaration: PsiElement?,
            qualifyNestedClasses: Boolean,
            includeClassTypeArguments: Boolean
    ): LookupElement {
        if (descriptor is ClassifierDescriptor &&
            declaration is PsiClass &&
            declaration !is KotlinLightClass) {
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
            nameAndIconDescriptor = descriptor.getContainingDeclaration()
            iconDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(nameAndIconDescriptor)
        }
        else {
            nameAndIconDescriptor = descriptor
            iconDeclaration = declaration
        }
        val name = nameAndIconDescriptor.getName().asString()

        val lookupObject = object : DeclarationLookupObjectImpl(descriptor, declaration, resolutionFacade) {
            override fun getIcon(flags: Int) = JetDescriptorIconProvider.getIcon(nameAndIconDescriptor, iconDeclaration, flags)
        }
        var element = LookupElementBuilder.create(lookupObject, name)

        val insertHandler = getDefaultInsertHandler(descriptor)
        element = element.withInsertHandler(insertHandler)

        when (descriptor) {
            is FunctionDescriptor -> {
                val returnType = descriptor.getReturnType()
                element = element.withTypeText(if (returnType != null) DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(returnType) else "")

                val insertsLambda = (insertHandler as KotlinFunctionInsertHandler).lambdaInfo != null
                if (insertsLambda) {
                    element = element.appendTailText(" {...} ", false)
                }

                element = element.appendTailText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderFunctionParameters(descriptor), insertsLambda)
            }

            is VariableDescriptor -> {
                element = element.withTypeText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(descriptor.getType()))
            }

            is ClassDescriptor -> {
                val typeParams = descriptor.getTypeConstructor().getParameters()
                if (includeClassTypeArguments && typeParams.isNotEmpty()) {
                    element = element.appendTailText(typeParams.map { it.getName().asString() }.joinToString(", ", "<", ">"), true)
                }

                var container = descriptor.getContainingDeclaration()

                if (qualifyNestedClasses) {
                    element = element.withPresentableText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderClassifierName(descriptor))

                    while (container is ClassDescriptor) {
                        container = container.getContainingDeclaration()
                    }
                }

                if (container is PackageFragmentDescriptor || container is ClassDescriptor) {
                    element = element.appendTailText(" (" + DescriptorUtils.getFqName(container) + ")", true)
                }
            }

            else -> {
                element = element.withTypeText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(descriptor))
            }
        }

        if (descriptor is CallableDescriptor) {
            val extensionReceiver = descriptor.original.extensionReceiverParameter
            when {
                descriptor is SyntheticJavaPropertyDescriptor -> {
                    var from = descriptor.getMethod.getName().asString() + "()"
                    descriptor.setMethod?.let { from += "/" + it.getName().asString() + "()" }
                    element = element.appendTailText(" (from $from)", true)
                }

                // no need to show them as extensions
                descriptor is SamAdapterExtensionFunctionDescriptor -> {}

                extensionReceiver != null -> {
                    val receiverPresentation = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(extensionReceiver.type)
                    element = element.appendTailText(" for $receiverPresentation", true)

                    val container = descriptor.getContainingDeclaration()
                    val containerPresentation = if (container is ClassDescriptor)
                        DescriptorUtils.getFqNameFromTopLevelClass(container).toString()
                    else if (container is PackageFragmentDescriptor)
                        container.fqName.toString()
                    else
                        null
                    if (containerPresentation != null) {
                        element = element.appendTailText(" in $containerPresentation", true)
                    }
                }

                else -> {
                    val container = descriptor.getContainingDeclaration()
                    if (container is PackageFragmentDescriptor) { // we show container only for global functions and properties
                        //TODO: it would be probably better to show it also for static declarations which are not from the current class (imported)
                        element = element.appendTailText(" (${container.fqName})", true)
                    }
                }
            }
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

        if (insertHandler is KotlinFunctionInsertHandler && insertHandler.lambdaInfo != null) {
            element.putUserData(KotlinCompletionCharFilter.ACCEPT_OPENING_BRACE, Unit)
        }

        return element.withIconFromLookupObject()
    }

    private fun LookupElement.withIconFromLookupObject(): LookupElement {
        // add icon in renderElement only to pass presentation.isReal()
        return object : LookupElementDecorator<LookupElement>(this) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.setIcon(DefaultLookupItemRenderer.getRawIcon(this@withIconFromLookupObject, presentation.isReal()))
            }
        }
    }

    private fun callableWeight(descriptor: DeclarationDescriptor): CallableWeight? {
        if (descriptor !is CallableDescriptor) return null

        val overridden = descriptor.overriddenDescriptors
        if (overridden.isNotEmpty()) {
            return overridden.map { callableWeight(it)!! }.min()!!
        }

        // don't treat synthetic extensions as real extensions
        if (descriptor is SyntheticJavaPropertyDescriptor) {
            return callableWeight(descriptor.getMethod)
        }
        if (descriptor is SamAdapterExtensionFunctionDescriptor) {
            return callableWeight(descriptor.sourceFunction)
        }

        val receiverParameter = descriptor.extensionReceiverParameter ?: descriptor.dispatchReceiverParameter
        if (receiverParameter != null) {
            return if (receiverTypes.any { TypeUtils.equalTypes(it, receiverParameter.type) }) {
                when {
                    descriptor.isExtensionForTypeParameter() -> CallableWeight.typeParameterExtension
                    descriptor.isExtension -> CallableWeight.thisTypeExtension
                    else -> CallableWeight.thisClassMember
                }
            }
            else if (receiverTypes.any { it.isSubtypeOf(receiverParameter.type) }) {
                if (descriptor.isExtension) CallableWeight.baseTypeExtension else CallableWeight.baseClassMember
            }
            else {
                CallableWeight.receiverCastRequired
            }
        }

        return when (descriptor.containingDeclaration) {
            is PackageFragmentDescriptor, is ClassifierDescriptor -> CallableWeight.globalOrStatic
            else -> CallableWeight.local
        }
    }

    private fun CallableDescriptor.isExtensionForTypeParameter(): Boolean {
        val receiverParameter = original.extensionReceiverParameter ?: return false
        val typeParameter = receiverParameter.type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
        return typeParameter.containingDeclaration == original
    }

    public fun getDefaultInsertHandler(descriptor: DeclarationDescriptor): InsertHandler<LookupElement> {
        return when (descriptor) {
            is FunctionDescriptor -> {
                val needTypeArguments = needTypeArguments(descriptor)
                val parameters = descriptor.valueParameters
                when (parameters.size()) {
                    0 -> KotlinFunctionInsertHandler(needTypeArguments, needValueArguments = false)

                    1 -> {
                        val parameterType = parameters.single().getType()
                        if (KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(parameterType)) {
                            val parameterCount = KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(parameterType).size()
                            if (parameterCount <= 1) {
                                // otherwise additional item with lambda template is to be added
                                return KotlinFunctionInsertHandler(needTypeArguments, needValueArguments = true, lambdaInfo = GenerateLambdaInfo(parameterType, false))
                            }
                        }
                        KotlinFunctionInsertHandler(needTypeArguments, needValueArguments = true)
                    }

                    else -> KotlinFunctionInsertHandler(needTypeArguments, needValueArguments = true)
                }
            }

            is PropertyDescriptor -> KotlinPropertyInsertHandler

            is ClassifierDescriptor -> KotlinClassifierInsertHandler

            else -> BaseDeclarationInsertHandler()
        }
    }

    private fun needTypeArguments(function: FunctionDescriptor): Boolean {
        if (function.typeParameters.isEmpty()) return false

        val originalFunction = function.original
        val typeParameters = originalFunction.typeParameters

        val potentiallyInferred = HashSet<TypeParameterDescriptor>()

        fun addPotentiallyInferred(type: JetType) {
            val descriptor = type.constructor.declarationDescriptor as? TypeParameterDescriptor
            if (descriptor != null && descriptor in typeParameters) {
                potentiallyInferred.add(descriptor)
            }

            if (KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(type) && KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(type).size() <= 1) {
                // do not rely on inference from input of function type with one or no arguments - use only return type of functional type
                addPotentiallyInferred(KotlinBuiltIns.getReturnTypeFromFunctionType(type))
                return
            }

            for (argument in type.arguments) {
                if (!argument.isStarProjection) { // otherwise we can fall into infinite recursion
                    addPotentiallyInferred(argument.type)
                }
            }
        }

        originalFunction.extensionReceiverParameter?.type?.let { addPotentiallyInferred(it) }
        originalFunction.valueParameters.forEach { addPotentiallyInferred(it.type) }

        fun allTypeParametersPotentiallyInferred() = originalFunction.typeParameters.all { it in potentiallyInferred }

        if (allTypeParametersPotentiallyInferred()) return false

        val returnType = originalFunction.returnType
        // check that there is an expected type and return value from the function can potentially match it
        if (returnType != null) {
            addPotentiallyInferred(returnType)

            if (allTypeParametersPotentiallyInferred() && expectedInfos.any { it.fuzzyType?.checkIsSuperTypeOf(originalFunction.fuzzyReturnType()!!) != null }) {
                return false
            }
        }

        return true
    }
}
