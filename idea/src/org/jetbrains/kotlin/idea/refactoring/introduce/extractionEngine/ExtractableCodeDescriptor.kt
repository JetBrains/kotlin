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
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.ErrorMessage
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.AnalysisResult.Status
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.ExpressionValue
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.Initializer
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.Jump
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference.ShorteningMode
import org.jetbrains.kotlin.idea.util.approximateFlexibleTypes
import org.jetbrains.kotlin.idea.util.isAnnotatedNotNull
import org.jetbrains.kotlin.idea.util.isAnnotatedNullable
import org.jetbrains.kotlin.idea.util.isUnit
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiRange
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.types.*
import java.util.Collections
import kotlin.properties.Delegates

trait Parameter {
    val argumentText: String
    val originalDescriptor: DeclarationDescriptor
    val name: String
    val mirrorVarName: String?
    val receiverCandidate: Boolean

    fun getParameterType(allowSpecialClassNames: Boolean): JetType

    fun getParameterTypeCandidates(allowSpecialClassNames: Boolean): List<JetType>

    fun copy(name: String, parameterType: JetType): Parameter
}

val Parameter.nameForRef: String get() = mirrorVarName ?: name

data class TypeParameter(
        val originalDeclaration: JetTypeParameter,
        val originalConstraints: List<JetTypeConstraint>
)

trait Replacement: Function1<JetElement, JetElement>

trait ParameterReplacement : Replacement {
    val parameter: Parameter
    fun copy(parameter: Parameter): ParameterReplacement
}

class RenameReplacement(override val parameter: Parameter): ParameterReplacement {
    override fun copy(parameter: Parameter) = RenameReplacement(parameter)

    override fun invoke(e: JetElement): JetElement {
        var expressionToReplace = (e.getParent() as? JetThisExpression ?: e).getQualifiedExpressionForSelectorOrThis()
        val parameterName = JetPsiUtil.unquoteIdentifier(parameter.nameForRef)
        val replacingName =
                if (e.getText().startsWith('`') || !KotlinNameSuggester.isIdentifier(parameterName)) "`$parameterName`" else parameterName
        val psiFactory = JetPsiFactory(e)
        val replacement =
                if (expressionToReplace is JetOperationReferenceExpression) {
                    psiFactory.createOperationName(replacingName)
                }
                else {
                    psiFactory.createSimpleName(replacingName)
                }
        return expressionToReplace.replaced(replacement)
    }
}

class AddPrefixReplacement(override val parameter: Parameter): ParameterReplacement {
    override fun copy(parameter: Parameter) = AddPrefixReplacement(parameter)

    override fun invoke(e: JetElement): JetElement {
        val selector = (e.getParent() as? JetCallExpression) ?: e
        val newExpr = selector.replace(JetPsiFactory(e).createExpression("${parameter.nameForRef}.${selector.getText()}")
        ) as JetQualifiedExpression

        return with(newExpr.getSelectorExpression()!!) { if (this is JetCallExpression) getCalleeExpression()!! else this }
    }
}

class FqNameReplacement(val fqName: FqName): Replacement {
    override fun invoke(e: JetElement): JetElement {
        val thisExpr = e.getParent() as? JetThisExpression
        if (thisExpr != null) {
            return thisExpr.replaced(JetPsiFactory(e).createExpression(fqName.asString())).getQualifiedElementSelector()!!
        }

        val newExpr = (e.getReference() as? JetSimpleNameReference)?.bindToFqName(fqName, ShorteningMode.NO_SHORTENING) as JetElement
        return if (newExpr is JetQualifiedExpression) newExpr.getSelectorExpression()!! else newExpr
    }
}

trait OutputValue {
    val originalExpressions: List<JetExpression>
    val valueType: JetType

    class ExpressionValue(
            val callSiteReturn: Boolean,
            override val originalExpressions: List<JetExpression>,
            override val valueType: JetType
    ): OutputValue

    class Jump(
            val elementsToReplace: List<JetExpression>,
            val elementToInsertAfterCall: JetElement?,
            val conditional: Boolean
    ): OutputValue {
        override val originalExpressions: List<JetExpression> get() = elementsToReplace
        override val valueType: JetType = with(KotlinBuiltIns.getInstance()) { if (conditional) getBooleanType() else getUnitType() }
    }

    class ParameterUpdate(
            val parameter: Parameter,
            override val originalExpressions: List<JetExpression>
    ): OutputValue {
        override val valueType: JetType get() = parameter.getParameterType(false)
    }

    class Initializer(
            val initializedDeclaration: JetProperty,
            override val valueType: JetType
    ): OutputValue {
        override val originalExpressions: List<JetExpression> get() = Collections.singletonList(initializedDeclaration)
    }
}

abstract class OutputValueBoxer(val outputValues: List<OutputValue>) {
    val outputValueTypes: List<JetType> get() = outputValues.map { it.valueType }

    abstract val returnType: JetType

    protected abstract fun getBoxingExpressionText(arguments: List<String>): String?

    abstract val boxingRequired: Boolean

    fun getReturnExpression(arguments: List<String>, psiFactory: JetPsiFactory): JetReturnExpression? {
        val expressionText = getBoxingExpressionText(arguments) ?: return null
        return psiFactory.createExpression("return $expressionText") as JetReturnExpression
    }

    protected abstract fun extractExpressionByIndex(boxedExpression: JetExpression, index: Int): JetExpression?

    protected fun extractArgumentExpressionByIndex(boxedExpression: JetExpression, index: Int): JetExpression? {
        val call: JetCallExpression? = when (boxedExpression) {
            is JetCallExpression -> boxedExpression
            is JetQualifiedExpression -> boxedExpression.getSelectorExpression() as? JetCallExpression
            else -> null
        }
        val arguments = call?.getValueArguments()
        if (arguments == null || arguments.size() <= index) return null

        return arguments[index].getArgumentExpression()
    }

    fun extractExpressionByValue(boxedExpression: JetExpression, value: OutputValue): JetExpression? {
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
            assert(outputValues.size() <= 3, "At most 3 output values are supported")
        }

        companion object {
            private val selectors = arrayOf("first", "second", "third")
        }

        override val returnType: JetType by Delegates.lazy {
            fun getType(): JetType {
                val boxingClass = when (outputValues.size()) {
                    1 -> return outputValues.first().valueType
                    2 -> module.resolveTopLevelClass(FqName("kotlin.Pair"))!!
                    3 -> module.resolveTopLevelClass(FqName("kotlin.Triple"))!!
                    else -> return DEFAULT_RETURN_TYPE
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

        override fun extractExpressionByIndex(boxedExpression: JetExpression, index: Int): JetExpression? {
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
        override val returnType: JetType by Delegates.lazy {
            if (outputValues.isEmpty()) DEFAULT_RETURN_TYPE
            else TypeUtils.substituteParameters(
                    KotlinBuiltIns.getInstance().getList(),
                    Collections.singletonList(CommonSupertypes.commonSupertype(outputValues.map { it.valueType }))
            )
        }

        override val boxingRequired: Boolean = outputValues.size() > 0

        override fun getBoxingExpressionText(arguments: List<String>): String? {
            if (arguments.isEmpty()) return null
            return arguments.joinToString(prefix = "kotlin.listOf(", separator = ", ", postfix = ")")
        }

        override fun extractExpressionByIndex(boxedExpression: JetExpression, index: Int): JetExpression? {
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
        val declarationsToCopy: List<JetDeclaration>
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

val ControlFlow.possibleReturnTypes: List<JetType>
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
        val returnType: JetType
) {
    val name: String get() = suggestedNames.firstOrNull() ?: ""
    val duplicates: List<DuplicateInfo> by Delegates.lazy { findDuplicates() }
}

enum class ExtractionTarget(val name: String) {
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
            val parent = descriptor.extractionData.targetSibling.getStrictParentOfType<JetDeclaration>()
            return !(parent is JetClass && parent.isInterface())
        }

        fun checkSimpleBody(descriptor: ExtractableCodeDescriptor): Boolean {
            val expression = descriptor.extractionData.getExpressions().singleOrNull()
            return expression != null && expression !is JetDeclaration && expression !is JetBlockExpression
        }

        fun checkSimpleControlFlow(descriptor: ExtractableCodeDescriptor): Boolean {
            val outputValue = descriptor.controlFlow.outputValues.singleOrNull()
            return (outputValue is ExpressionValue && !outputValue.callSiteReturn) || outputValue is Initializer
        }

        fun checkSignatureAndParent(descriptor: ExtractableCodeDescriptor): Boolean {
            if (!descriptor.parameters.isEmpty()) return false
            if (descriptor.returnType.isUnit()) return false

            val parent = descriptor.extractionData.targetSibling.getParent()
            return (parent is JetFile || parent is JetClassBody)
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
        val declaration: JetNamedDeclaration,
        val duplicateReplacers: Map<JetPsiRange, () -> Unit>,
        val nameByOffset: Map<Int, JetElement>
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
            val message = JetRefactoringBundle.message(
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
