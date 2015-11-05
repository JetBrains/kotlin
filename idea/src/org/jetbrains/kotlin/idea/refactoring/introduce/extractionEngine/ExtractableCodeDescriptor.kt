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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.ErrorMessage
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.Status
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.ExpressionValue
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.Initializer
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.Jump
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference.ShorteningMode
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.approximateFlexibleTypes
import org.jetbrains.kotlin.idea.util.isAnnotatedNotNull
import org.jetbrains.kotlin.idea.util.isAnnotatedNullable
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiRange
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isUnit
import java.util.Collections

interface Parameter {
    val argumentText: String
    val originalDescriptor: DeclarationDescriptor
    val name: String
    val mirrorVarName: String?
    val receiverCandidate: Boolean

    fun getParameterType(allowSpecialClassNames: Boolean): KotlinType

    fun getParameterTypeCandidates(allowSpecialClassNames: Boolean): List<KotlinType>

    fun copy(name: String, parameterType: KotlinType): Parameter
}

val Parameter.nameForRef: String get() = mirrorVarName ?: name

data class TypeParameter(
        val originalDeclaration: KtTypeParameter,
        val originalConstraints: List<KtTypeConstraint>
)

interface Replacement: Function2<ExtractableCodeDescriptor, KtElement, KtElement>

interface ParameterReplacement : Replacement {
    val parameter: Parameter
    fun copy(parameter: Parameter): ParameterReplacement
}

class RenameReplacement(override val parameter: Parameter): ParameterReplacement {
    override fun copy(parameter: Parameter) = RenameReplacement(parameter)

    override fun invoke(descriptor: ExtractableCodeDescriptor, e: KtElement): KtElement {
        var expressionToReplace = (e.getParent() as? KtThisExpression ?: e).let { it.getQualifiedExpressionForSelector() ?: it }
        val parameterName = KtPsiUtil.unquoteIdentifier(parameter.nameForRef)
        val replacingName =
                if (e.getText().startsWith('`') || !KotlinNameSuggester.isIdentifier(parameterName)) "`$parameterName`" else parameterName
        val psiFactory = KtPsiFactory(e)
        val replacement = when {
            parameter == descriptor.receiverParameter -> psiFactory.createExpression("this")
            expressionToReplace is KtOperationReferenceExpression -> psiFactory.createOperationName(replacingName)
            else -> psiFactory.createSimpleName(replacingName)
        }
        return expressionToReplace.replaced(replacement)
    }
}

class AddPrefixReplacement(override val parameter: Parameter): ParameterReplacement {
    override fun copy(parameter: Parameter) = AddPrefixReplacement(parameter)

    override fun invoke(descriptor: ExtractableCodeDescriptor, e: KtElement): KtElement {
        if (descriptor.receiverParameter == parameter) return e

        val selector = (e.parent as? KtCallExpression) ?: e
        val replacingExpression = KtPsiFactory(e).createExpression("${parameter.nameForRef}.${selector.text}")
        val newExpr = (selector.replace(replacingExpression) as KtQualifiedExpression).selectorExpression!!
        return (newExpr as? KtCallExpression)?.calleeExpression ?: newExpr
    }
}

class FqNameReplacement(val fqName: FqName): Replacement {
    override fun invoke(descriptor: ExtractableCodeDescriptor, e: KtElement): KtElement {
        val thisExpr = e.getParent() as? KtThisExpression
        if (thisExpr != null) {
            return thisExpr.replaced(KtPsiFactory(e).createExpression(fqName.asString())).getQualifiedElementSelector()!!
        }

        val newExpr = (e as? KtSimpleNameExpression)?.mainReference?.bindToFqName(fqName, ShorteningMode.NO_SHORTENING) as KtElement
        return if (newExpr is KtQualifiedExpression) newExpr.getSelectorExpression()!! else newExpr
    }
}

interface OutputValue {
    val originalExpressions: List<KtExpression>
    val valueType: KotlinType

    class ExpressionValue(
            val callSiteReturn: Boolean,
            override val originalExpressions: List<KtExpression>,
            override val valueType: KotlinType
    ): OutputValue

    class Jump(
            val elementsToReplace: List<KtExpression>,
            val elementToInsertAfterCall: KtElement?,
            val conditional: Boolean,
            private val builtIns: KotlinBuiltIns
    ): OutputValue {
        override val originalExpressions: List<KtExpression> get() = elementsToReplace
        override val valueType: KotlinType = with(builtIns) { if (conditional) booleanType else unitType }
    }

    class ParameterUpdate(
            val parameter: Parameter,
            override val originalExpressions: List<KtExpression>
    ): OutputValue {
        override val valueType: KotlinType get() = parameter.getParameterType(false)
    }

    class Initializer(
            val initializedDeclaration: KtProperty,
            override val valueType: KotlinType
    ): OutputValue {
        override val originalExpressions: List<KtExpression> get() = Collections.singletonList(initializedDeclaration)
    }
}

abstract class OutputValueBoxer(val outputValues: List<OutputValue>) {
    val outputValueTypes: List<KotlinType> get() = outputValues.map { it.valueType }

    abstract val returnType: KotlinType

    protected abstract fun getBoxingExpressionText(arguments: List<String>): String?

    abstract val boxingRequired: Boolean

    fun getReturnExpression(arguments: List<String>, psiFactory: KtPsiFactory): KtReturnExpression? {
        val expressionText = getBoxingExpressionText(arguments) ?: return null
        return psiFactory.createExpression("return $expressionText") as KtReturnExpression
    }

    protected abstract fun extractExpressionByIndex(boxedExpression: KtExpression, index: Int): KtExpression?

    protected fun extractArgumentExpressionByIndex(boxedExpression: KtExpression, index: Int): KtExpression? {
        val call: KtCallExpression? = when (boxedExpression) {
            is KtCallExpression -> boxedExpression
            is KtQualifiedExpression -> boxedExpression.getSelectorExpression() as? KtCallExpression
            else -> null
        }
        val arguments = call?.getValueArguments()
        if (arguments == null || arguments.size() <= index) return null

        return arguments[index].getArgumentExpression()
    }

    fun extractExpressionByValue(boxedExpression: KtExpression, value: OutputValue): KtExpression? {
        val index = outputValues.indexOf(value)
        if (index < 0) return null

        return extractExpressionByIndex(boxedExpression, index)
    }

    abstract fun getUnboxingExpressions(boxedText: String): Map<OutputValue, String>

    class AsTuple(
            outputValues: List<OutputValue>,
            val module: ModuleDescriptor
    ) : OutputValueBoxer(outputValues) {
        init {
            assert(outputValues.size() <= 3) { "At most 3 output values are supported" }
        }

        companion object {
            private val selectors = arrayOf("first", "second", "third")
        }

        override val returnType: KotlinType by lazy {
            fun getType(): KotlinType {
                val boxingClass = when (outputValues.size()) {
                    1 -> return outputValues.first().valueType
                    2 -> module.resolveTopLevelClass(FqName("kotlin.Pair"), NoLookupLocation.FROM_IDE)!!
                    3 -> module.resolveTopLevelClass(FqName("kotlin.Triple"), NoLookupLocation.FROM_IDE)!!
                    else -> return module.builtIns.defaultReturnType
                }
                return TypeUtils.substituteParameters(boxingClass, outputValueTypes)
            }

            getType()
        }

        override val boxingRequired: Boolean = outputValues.size() > 1

        override fun getBoxingExpressionText(arguments: List<String>): String? {
            return when (arguments.size()) {
                0 -> null
                1 -> arguments.first()
                else -> {
                    val constructorName = DescriptorUtils.getFqName(returnType.getConstructor().getDeclarationDescriptor()!!).asString()
                    return arguments.joinToString(prefix = "$constructorName(", separator = ", ", postfix = ")")
                }
            }
        }

        override fun extractExpressionByIndex(boxedExpression: KtExpression, index: Int): KtExpression? {
            if (outputValues.size() == 1) return boxedExpression
            return extractArgumentExpressionByIndex(boxedExpression, index)
        }

        override fun getUnboxingExpressions(boxedText: String): Map<OutputValue, String> {
            return when (outputValues.size()) {
                0 -> Collections.emptyMap()
                1 -> Collections.singletonMap(outputValues.first(), boxedText)
                else -> {
                    var i = 0
                    ContainerUtil.newMapFromKeys(outputValues.iterator()) { "$boxedText.${selectors[i++]}" }
                }
            }
        }
    }

    class AsList(outputValues: List<OutputValue>): OutputValueBoxer(outputValues) {
        override val returnType: KotlinType by lazy {
            assert(outputValues.isNotEmpty())
            val builtIns = outputValues.first().valueType.builtIns
            TypeUtils.substituteParameters(
                    builtIns.list,
                    Collections.singletonList(CommonSupertypes.commonSupertype(outputValues.map { it.valueType }))
            )
        }

        override val boxingRequired: Boolean = outputValues.size() > 0

        override fun getBoxingExpressionText(arguments: List<String>): String? {
            if (arguments.isEmpty()) return null
            return arguments.joinToString(prefix = "kotlin.listOf(", separator = ", ", postfix = ")")
        }

        override fun extractExpressionByIndex(boxedExpression: KtExpression, index: Int): KtExpression? {
            return extractArgumentExpressionByIndex(boxedExpression, index)
        }

        override fun getUnboxingExpressions(boxedText: String): Map<OutputValue, String> {
            var i = 0
            return ContainerUtil.newMapFromKeys(outputValues.iterator()) { "$boxedText[${i++}]" }
        }
    }
}

data class ControlFlow(
        val outputValues: List<OutputValue>,
        val boxerFactory: (List<OutputValue>) -> OutputValueBoxer,
        val declarationsToCopy: List<KtDeclaration>
) {
    val outputValueBoxer = boxerFactory(outputValues)

    val defaultOutputValue: ExpressionValue? = with(outputValues.filterIsInstance<ExpressionValue>()) {
        if (size() > 1) throw IllegalArgumentException("Multiple expression values: ${outputValues.joinToString()}") else firstOrNull()
    }

    val jumpOutputValue: Jump? = with(outputValues.filterIsInstance<Jump>()) {
        val jumpCount = size()
        when {
            isEmpty() ->
                null
            outputValues.size() > jumpCount || jumpCount > 1 ->
                throw IllegalArgumentException("Jump values must be the only value if it's present: ${outputValues.joinToString()}")
            else ->
                first()
        }
    }
}

val ControlFlow.possibleReturnTypes: List<KotlinType>
    get() {
        val returnType = outputValueBoxer.returnType
        return when {
            !returnType.isNullabilityFlexible() ->
                listOf(returnType)
            returnType.isAnnotatedNotNull(), returnType.isAnnotatedNullable() ->
                listOf(approximateFlexibleTypes(returnType))
            else ->
                returnType.getCapability(javaClass<Flexibility>()).let { listOf(it!!.upperBound, it.lowerBound) }
        }
    }

fun ControlFlow.toDefault(): ControlFlow =
        copy(outputValues = outputValues.filterNot { it is OutputValue.Jump || it is OutputValue.ExpressionValue })

data class ExtractableCodeDescriptor(
        val extractionData: ExtractionData,
        val originalContext: BindingContext,
        val suggestedNames: List<String>,
        val visibility: String,
        val parameters: List<Parameter>,
        val receiverParameter: Parameter?,
        val typeParameters: List<TypeParameter>,
        val replacementMap: Map<Int, Replacement>,
        val controlFlow: ControlFlow,
        val returnType: KotlinType
) {
    val name: String get() = suggestedNames.firstOrNull() ?: ""
    val duplicates: List<DuplicateInfo> by lazy { findDuplicates() }
}

enum class ExtractionTarget(val targetName: String) {
    FUNCTION("function") {
        override fun isAvailable(descriptor: ExtractableCodeDescriptor) = true
    },

    FAKE_LAMBDALIKE_FUNCTION("lambda parameter") {
        override fun isAvailable(descriptor: ExtractableCodeDescriptor): Boolean {
            return checkSimpleControlFlow(descriptor) || descriptor.controlFlow.outputValues.isEmpty()
        }
    },

    PROPERTY_WITH_INITIALIZER("property with initializer") {
        override fun isAvailable(descriptor: ExtractableCodeDescriptor): Boolean {
            return checkSignatureAndParent(descriptor)
                   && checkSimpleControlFlow(descriptor)
                   && checkSimpleBody(descriptor)
                   && checkNotTrait(descriptor)
                   && descriptor.receiverParameter == null
        }
    },

    PROPERTY_WITH_GETTER("property with getter") {
        override fun isAvailable(descriptor: ExtractableCodeDescriptor): Boolean {
            return checkSignatureAndParent(descriptor)
        }
    },

    LAZY_PROPERTY("lazy property") {
        override fun isAvailable(descriptor: ExtractableCodeDescriptor): Boolean {
            return checkSignatureAndParent(descriptor)
                   && checkSimpleControlFlow(descriptor)
                   && checkNotTrait(descriptor)
                   && descriptor.receiverParameter == null
        }
    };

    abstract fun isAvailable(descriptor: ExtractableCodeDescriptor): Boolean

    companion object {
        fun checkNotTrait(descriptor: ExtractableCodeDescriptor): Boolean {
            val parent = descriptor.extractionData.targetSibling.getStrictParentOfType<KtDeclaration>()
            return !(parent is KtClass && parent.isInterface())
        }

        fun checkSimpleBody(descriptor: ExtractableCodeDescriptor): Boolean {
            val expression = descriptor.extractionData.getExpressions().singleOrNull()
            return expression != null && expression !is KtDeclaration && expression !is KtBlockExpression
        }

        fun checkSimpleControlFlow(descriptor: ExtractableCodeDescriptor): Boolean {
            val outputValue = descriptor.controlFlow.outputValues.singleOrNull()
            return (outputValue is ExpressionValue && !outputValue.callSiteReturn) || outputValue is Initializer
        }

        fun checkSignatureAndParent(descriptor: ExtractableCodeDescriptor): Boolean {
            if (!descriptor.parameters.isEmpty()) return false
            if (descriptor.returnType.isUnit()) return false

            val parent = descriptor.extractionData.targetSibling.getParent()
            return (parent is KtFile || parent is KtClassBody)
        }
    }
}

val propertyTargets: List<ExtractionTarget> = listOf(ExtractionTarget.PROPERTY_WITH_INITIALIZER,
                                                     ExtractionTarget.PROPERTY_WITH_GETTER,
                                                     ExtractionTarget.LAZY_PROPERTY)

data class ExtractionGeneratorOptions(
        val inTempFile: Boolean = false,
        val target: ExtractionTarget = ExtractionTarget.FUNCTION,
        val flexibleTypesAllowed: Boolean = false,
        val allowDummyName: Boolean = false,
        val allowExpressionBody: Boolean = true
) {
    companion object {
        val DEFAULT = ExtractionGeneratorOptions()
    }
}

data class ExtractionGeneratorConfiguration(
        val descriptor: ExtractableCodeDescriptor,
        val generatorOptions: ExtractionGeneratorOptions
)

data class ExtractionResult(
        val config: ExtractionGeneratorConfiguration,
        val declaration: KtNamedDeclaration,
        val duplicateReplacers: Map<KotlinPsiRange, () -> Unit>,
        val nameByOffset: Map<Int, KtElement>
)

class AnalysisResult (
        val descriptor: ExtractableCodeDescriptor?,
        val status: Status,
        val messages: List<ErrorMessage>
) {
    enum class Status {
        SUCCESS,
        NON_CRITICAL_ERROR,
        CRITICAL_ERROR
    }

    enum class ErrorMessage {
        NO_EXPRESSION,
        NO_CONTAINER,
        SUPER_CALL,
        DENOTABLE_TYPES,
        ERROR_TYPES,
        MULTIPLE_OUTPUT,
        OUTPUT_AND_EXIT_POINT,
        MULTIPLE_EXIT_POINTS,
        DECLARATIONS_ARE_USED_OUTSIDE,
        DECLARATIONS_OUT_OF_SCOPE;

        var additionalInfo: List<String>? = null

        fun addAdditionalInfo(info: List<String>): ErrorMessage {
            additionalInfo = info
            return this
        }

        fun renderMessage(): String {
            val message = KotlinRefactoringBundle.message(
                    when (this) {
                        NO_EXPRESSION -> "cannot.refactor.no.expression"
                        NO_CONTAINER -> "cannot.refactor.no.container"
                        SUPER_CALL -> "cannot.extract.super.call"
                        DENOTABLE_TYPES -> "parameter.types.are.not.denotable"
                        ERROR_TYPES -> "error.types.in.generated.function"
                        MULTIPLE_OUTPUT -> "selected.code.fragment.has.multiple.output.values"
                        OUTPUT_AND_EXIT_POINT -> "selected.code.fragment.has.output.values.and.exit.points"
                        MULTIPLE_EXIT_POINTS -> "selected.code.fragment.has.multiple.exit.points"
                        DECLARATIONS_ARE_USED_OUTSIDE -> "declarations.are.used.outside.of.selected.code.fragment"
                        DECLARATIONS_OUT_OF_SCOPE -> "declarations.will.move.out.of.scope"
                    }
            )

            return additionalInfo?.let { "$message\n\n${it.map { StringUtil.htmlEmphasize(it) }.joinToString("\n")}" } ?: message
        }
    }
}

class ExtractableCodeDescriptorWithConflicts(
        val descriptor: ExtractableCodeDescriptor,
        val conflicts: MultiMap<PsiElement, String>
)
