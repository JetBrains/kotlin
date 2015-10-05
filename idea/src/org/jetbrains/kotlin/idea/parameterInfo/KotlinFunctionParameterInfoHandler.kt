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

package org.jetbrains.kotlin.idea.parameterInfo

import com.google.common.collect.Iterables
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.ASTNode
import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.util.Pair
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import java.awt.Color
import java.util.*

class KotlinFunctionParameterInfoHandler : ParameterInfoHandlerWithTabActionSupport<JetValueArgumentList, Pair<out FunctionDescriptor, ResolutionFacade>, JetValueArgument> {

    override fun getActualParameters(arguments: JetValueArgumentList) = arguments.arguments.toTypedArray()

    override fun getActualParameterDelimiterType() = JetTokens.COMMA

    override fun getActualParametersRBraceType() = JetTokens.RBRACE

    override fun getArgumentListAllowedParentClasses() = setOf(JetCallElement::class.java)

    override fun getArgListStopSearchClasses() = setOf(JetFunction::class.java)

    override fun getArgumentListClass() = JetValueArgumentList::class.java

    override fun couldShowInLookup() = true

    override fun getParametersForLookup(item: LookupElement, context: ParameterInfoContext) = emptyArray<Any>() //todo: ?

    override fun getParametersForDocumentation(p: Pair<out FunctionDescriptor, ResolutionFacade>, context: ParameterInfoContext) = emptyArray<Any>() //todo: ?

    override fun findElementForParameterInfo(context: CreateParameterInfoContext): JetValueArgumentList? {
        return findCall(context)
    }

    override fun showParameterInfo(element: JetValueArgumentList, context: CreateParameterInfoContext) {
        context.showHint(element, element.textRange.startOffset, this)
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): JetValueArgumentList? {
        return findCallAndUpdateContext(context)
    }

    override fun updateParameterInfo(argumentList: JetValueArgumentList, context: UpdateParameterInfoContext) {
        if (context.parameterOwner !== argumentList) context.removeHint()
        val offset = context.offset
        var child: ASTNode? = argumentList.node.firstChildNode
        var i = 0
        while (child != null && child.startOffset < offset) {
            if (child.elementType === JetTokens.COMMA) ++i
            child = child.treeNext
        }
        context.setCurrentParameter(i)
    }

    override fun getParameterCloseChars() = ParameterInfoUtils.DEFAULT_PARAMETER_CLOSE_CHARS

    override fun tracksParameterIndex() = true

    override fun updateUI(itemToShow: Pair<out FunctionDescriptor, ResolutionFacade>, context: ParameterInfoUIContext) {
        //todo: when we will have ability to pass Array as vararg, implement such feature here too?
        if (context.parameterOwner == null || !context.parameterOwner.isValid) {
            context.isUIComponentEnabled = false
            return
        }

        val parameterOwner = context.parameterOwner
        if (parameterOwner !is JetValueArgumentList) {
            context.isUIComponentEnabled = false
            return
        }

        val functionDescriptor = itemToShow.first
        val resolutionFacade = itemToShow.second

        val valueParameters = functionDescriptor.valueParameters
        val valueArguments = parameterOwner.arguments

        val currentParameterIndex = context.currentParameterIndex
        var boldStartOffset = -1
        var boldEndOffset = -1
        var isGrey = false
        val isDeprecated = KotlinBuiltIns.isDeprecated(functionDescriptor)

        val usedIndexes = BooleanArray(valueParameters.size())
        Arrays.fill(usedIndexes, false)

        var namedMode = false

        if (!isIndexValid(valueParameters, currentParameterIndex)) {
            isGrey = true
        }

        val builder = StringBuilder()

        val owner = context.parameterOwner
        val bindingContext = resolutionFacade.analyze(owner as JetElement, BodyResolveMode.FULL)

        for (i in valueParameters.indices) {
            if (i != 0) {
                builder.append(", ")
            }

            val highlightParameter = i == currentParameterIndex || (!namedMode && i < currentParameterIndex && Iterables.getLast(valueParameters).varargElementType != null)

            if (highlightParameter) {
                boldStartOffset = builder.length()
            }

            if (!namedMode) {
                if (valueArguments.size() > i) {
                    val argument = valueArguments.get(i)
                    if (argument.isNamed()) {
                        namedMode = true
                    }
                    else {
                        val param = valueParameters.get(i)
                        builder.append(renderParameter(param, false))
                        if (i <= currentParameterIndex && !isArgumentTypeValid(bindingContext, argument, param)) {
                            isGrey = true
                        }
                        usedIndexes[i] = true
                    }
                }
                else {
                    val param = valueParameters.get(i)
                    builder.append(renderParameter(param, false))
                }
            }

            if (namedMode) {
                var takeAnyArgument = true
                if (valueArguments.size() > i) {
                    val argument = valueArguments.get(i)
                    if (argument.isNamed()) {
                        for (j in valueParameters.indices) {
                            val referenceExpression = argument.getArgumentName()!!.getReferenceExpression()
                            val param = valueParameters[j]
                            if (!usedIndexes[j] && param.name == referenceExpression.getReferencedNameAsName()) {
                                takeAnyArgument = false
                                usedIndexes[j] = true
                                builder.append(renderParameter(param, true))
                                if (i < currentParameterIndex && !isArgumentTypeValid(bindingContext, argument, param)) {
                                    isGrey = true
                                }
                                break
                            }
                        }
                    }
                }

                if (takeAnyArgument) {
                    if (i < currentParameterIndex) {
                        isGrey = true
                    }

                    for (j in valueParameters.indices) {
                        val param = valueParameters.get(j)
                        if (!usedIndexes[j]) {
                            usedIndexes[j] = true
                            builder.append(renderParameter(param, true))
                            break
                        }
                    }
                }
            }

            if (highlightParameter) {
                boldEndOffset = builder.length()
            }
        }

        if (valueParameters.size() == 0) {
            builder.append(CodeInsightBundle.message("parameter.info.no.parameters"))
        }

        assert(!builder.toString().isEmpty()) { "A message about 'no parameters' or some parameters should be present: " + functionDescriptor }

        val color = if (isResolvedToDescriptor(parameterOwner, functionDescriptor, bindingContext)) GREEN_BACKGROUND else context.defaultParameterColor
        context.setupUIComponentPresentation(builder.toString(), boldStartOffset, boldEndOffset, isGrey, isDeprecated, false, color)
    }

    companion object {
        val GREEN_BACKGROUND: Color = JBColor(Color(231, 254, 234), Gray._100)

        private fun renderParameter(parameter: ValueParameterDescriptor, named: Boolean): String {
            val builder = StringBuilder()
            if (named) builder.append("[")
            if (parameter.varargElementType != null) {
                builder.append("vararg ")
            }
            builder.append(parameter.name).append(": ").append(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(getActualParameterType(parameter)))
            if (parameter.hasDefaultValue()) {
                val parameterDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(parameter)
                builder.append(" = ").append(getDefaultExpressionString(parameterDeclaration))
            }
            if (named) builder.append("]")
            return builder.toString()
        }

        private fun getDefaultExpressionString(parameterDeclaration: PsiElement?): String {
            if (parameterDeclaration is JetParameter) {
                val defaultValue = parameterDeclaration.defaultValue
                if (defaultValue != null) {
                    val defaultExpression = defaultValue.text
                    if (defaultExpression.length() <= 32) {
                        return defaultExpression
                    }

                    if (defaultValue is JetConstantExpression || defaultValue is JetStringTemplateExpression) {
                        if (defaultExpression.startsWith("\"")) {
                            return "\"...\""
                        }
                        else if (defaultExpression.startsWith("\'")) {
                            return "\'...\'"
                        }
                    }
                }
            }
            return "..."
        }

        private fun getActualParameterType(descriptor: ValueParameterDescriptor)
                = descriptor.varargElementType ?: descriptor.type

        private fun isArgumentTypeValid(bindingContext: BindingContext, argument: JetValueArgument, param: ValueParameterDescriptor): Boolean {
            val expression = argument.getArgumentExpression() ?: return false
            val paramType = getActualParameterType(param)
            val exprType = bindingContext.getType(expression)
            return exprType == null || JetTypeChecker.DEFAULT.isSubtypeOf(exprType, paramType)
        }

        private fun isIndexValid(valueParameters: List<ValueParameterDescriptor>, index: Int): Boolean {
            // Index is within range of parameters or last parameter is vararg
            return index < valueParameters.size() || (valueParameters.size() > 0 && Iterables.getLast(valueParameters).varargElementType != null)
        }

        private fun isResolvedToDescriptor(
                argumentList: JetValueArgumentList,
                functionDescriptor: FunctionDescriptor,
                bindingContext: BindingContext): Boolean {
            val callNameExpression = getCallSimpleNameExpression(argumentList)
            if (callNameExpression != null) {
                val declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, callNameExpression)
                if (declarationDescriptor != null) {
                    if (declarationDescriptor === functionDescriptor) {
                        return true
                    }
                }
            }

            return false
        }

        private fun findCall(context: CreateParameterInfoContext): JetValueArgumentList? {
            //todo: calls to this constructors, when we will have auxiliary constructors
            val file = context.file
            if (file !is JetFile) {
                return null
            }

            val argumentList = PsiTreeUtil.getParentOfType(file.findElementAt(context.offset), JetValueArgumentList::class.java) ?: return null

            val callNameExpression = getCallSimpleNameExpression(argumentList) ?: return null

            val references = callNameExpression.references
            if (references.size() == 0) {
                return null
            }

            val resolutionFacade = callNameExpression.getContainingJetFile().getResolutionFacade()
            val bindingContext = resolutionFacade.analyze(callNameExpression, BodyResolveMode.FULL)

            val scope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, callNameExpression)
            val placeDescriptor = scope?.getContainingDeclaration()

            val visibilityFilter = { descriptor: DeclarationDescriptor ->
                placeDescriptor == null
                || descriptor !is DeclarationDescriptorWithVisibility
                || descriptor.isVisible(placeDescriptor, bindingContext, callNameExpression)
            }

            val refName = callNameExpression.getReferencedNameAsName()

            val descriptorKindFilter = DescriptorKindFilter(DescriptorKindFilter.FUNCTIONS_MASK or DescriptorKindFilter.CLASSIFIERS_MASK, emptyList<DescriptorKindExclude>())

            val variants = ReferenceVariantsHelper(bindingContext, resolutionFacade, visibilityFilter)
                    .getReferenceVariants(callNameExpression, descriptorKindFilter, { it == refName })

            val itemsToShow = ArrayList<Pair<out DeclarationDescriptor, ResolutionFacade>>()
            for (variant in variants) {
                if (variant is FunctionDescriptor) {
                    //todo: renamed functions?
                    itemsToShow.add(Pair.create(variant, resolutionFacade))
                }
                else if (variant is ClassDescriptor) {
                    //todo: renamed classes?
                    for (constructorDescriptor in variant.constructors) {
                        itemsToShow.add(Pair.create(constructorDescriptor, resolutionFacade))
                    }
                }
            }

            context.itemsToShow = ArrayUtil.toObjectArray(itemsToShow)
            return argumentList
        }

        private fun getCallSimpleNameExpression(argumentList: JetValueArgumentList): JetSimpleNameExpression? {
            val argumentListParent = argumentList.parent
            return if ((argumentListParent is JetCallElement))
                argumentListParent.getCallNameExpression()
            else
                null
        }

        private fun findCallAndUpdateContext(context: UpdateParameterInfoContext): JetValueArgumentList? {
            val file = context.file
            var element = file.findElementAt(context.offset) ?: return null
            var parent = element.parent
            while (parent != null && parent !is JetValueArgumentList) {
                element = element!!.parent
                parent = parent.parent
            }
            if (parent == null) return null
            val argumentList = parent as JetValueArgumentList
            if (element is JetValueArgument) {
                val i = argumentList.arguments.indexOf(element)
                context.setCurrentParameter(i)
                context.setHighlightedParameter(element)
            }
            return argumentList
        }
    }
}
