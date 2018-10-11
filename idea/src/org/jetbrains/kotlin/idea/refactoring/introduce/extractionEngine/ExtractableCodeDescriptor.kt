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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.OutputValue.*
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference.ShorteningMode
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.approximateFlexibleTypes
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiRange
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.nullability
import java.util.*

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
        var expressionToReplace = (e.parent as? KtThisExpression ?: e).let { it.getQualifiedExpressionForSelector() ?: it }
        val parameterName = KtPsiUtil.unquoteIdentifier(parameter.nameForRef)
        val replacingName =
                if (e.text.startsWith('`') || !parameterName.isIdentifier()) "`$parameterName`" else parameterName
        val psiFactory = KtPsiFactory(e)
        val replacement = when {
            parameter == descriptor.receiverParameter -> psiFactory.createExpression("this")
            expressionToReplace is KtOperationReferenceExpression -> psiFactory.createOperationName(replacingName)
            else -> psiFactory.createSimpleName(replacingName)
        }
        return expressionToReplace.replaced(replacement)
    }
}

abstract class WrapInWithReplacement : Replacement {
    abstract val argumentText: String

    override fun invoke(descriptor: ExtractableCodeDescriptor, e: KtElement): KtElement {
        val call = (e as? KtSimpleNameExpression)?.getQualifiedElement() ?: return e
        val replacingExpression = KtPsiFactory(e).createExpressionByPattern("with($0) { $1 }", argumentText, call)
        val replace = call.replace(replacingExpression)
        return (replace as KtCallExpression).lambdaArguments.first().getLambdaExpression()!!.bodyExpression!!.statements.first()
    }
}

class WrapParameterInWithReplacement(override val parameter: Parameter): WrapInWithReplacement(), ParameterReplacement {
    override val argumentText: String
        get() = parameter.name

    override fun copy(parameter: Parameter) = WrapParameterInWithReplacement(parameter)
}

class WrapObjectInWithReplacement(val descriptor: ClassDescriptor): WrapInWithReplacement() {
    override val argumentText: String
        get() = IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(descriptor)
}

class AddPrefixReplacement(override val parameter: Parameter): ParameterReplacement {
    override fun copy(parameter: Parameter) = AddPrefixReplacement(parameter)

    override fun invoke(descriptor: ExtractableCodeDescriptor, e: KtElement): KtElement {
        if (descriptor.receiverParameter == parameter) return e

        val selector = (e.parent as? KtCallExpression) ?: e
        val replacingExpression = KtPsiFactory(e).createExpressionByPattern("${parameter.nameForRef}.$0", selector)
        val newExpr = (selector.replace(replacingExpression) as KtQualifiedExpression).selectorExpression!!
        return (newExpr as? KtCallExpression)?.calleeExpression ?: newExpr
    }
}

class FqNameReplacement(val fqName: FqName): Replacement {
    override fun invoke(descriptor: ExtractableCodeDescriptor, e: KtElement): KtElement {
        val thisExpr = e.parent as? KtThisExpression
        if (thisExpr != null) {
            return thisExpr.replaced(KtPsiFactory(e).createExpression(fqName.asString())).getQualifiedElementSelector()!!
        }

        val newExpr = (e as? KtSimpleNameExpression)?.mainReference?.bindToFqName(fqName, ShorteningMode.NO_SHORTENING) as KtElement
        return if (newExpr is KtQualifiedExpression) newExpr.selectorExpression!! else newExpr
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
            builtIns: KotlinBuiltIns
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

    protected abstract fun getBoxingExpressionPattern(arguments: List<KtExpression>): String?

    abstract val boxingRequired: Boolean

    fun getReturnExpression(arguments: List<KtExpression>, psiFactory: KtPsiFactory): KtReturnExpression? {
        val expressionPattern = getBoxingExpressionPattern(arguments) ?: return null
        return psiFactory.createExpressionByPattern("return $expressionPattern", *arguments.toTypedArray()) as KtReturnExpression
    }

    protected abstract fun extractExpressionByIndex(boxedExpression: KtExpression, index: Int): KtExpression?

    protected fun extractArgumentExpressionByIndex(boxedExpression: KtExpression, index: Int): KtExpression? {
        val call: KtCallExpression? = when (boxedExpression) {
            is KtCallExpression -> boxedExpression
            is KtQualifiedExpression -> boxedExpression.selectorExpression as? KtCallExpression
            else -> null
        }
        val arguments = call?.valueArguments
        if (arguments == null || arguments.size <= index) return null

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
            assert(outputValues.size <= 3) { "At most 3 output values are supported" }
        }

        companion object {
            private val selectors = arrayOf("first", "second", "third")
        }

        override val returnType: KotlinType by lazy {
            fun getType(): KotlinType {
                val boxingClass = when (outputValues.size) {
                    1 -> return outputValues.first().valueType
                    2 -> module.resolveTopLevelClass(FqName("kotlin.Pair"), NoLookupLocation.FROM_IDE)!!
                    3 -> module.resolveTopLevelClass(FqName("kotlin.Triple"), NoLookupLocation.FROM_IDE)!!
                    else -> return module.builtIns.defaultReturnType
                }
                return TypeUtils.substituteParameters(boxingClass, outputValueTypes)
            }

            getType()
        }

        override val boxingRequired: Boolean = outputValues.size > 1

        override fun getBoxingExpressionPattern(arguments: List<KtExpression>): String? {
            return when (arguments.size) {
                0 -> null
                1 -> "$0"
                else -> {
                    val constructorName = DescriptorUtils.getFqName(returnType.constructor.declarationDescriptor!!).asString()
                    return arguments.indices.joinToString(prefix = "$constructorName(", separator = ", ", postfix = ")") { "\$$it" }
                }
            }
        }

        override fun extractExpressionByIndex(boxedExpression: KtExpression, index: Int): KtExpression? {
            if (outputValues.size == 1) return boxedExpression
            return extractArgumentExpressionByIndex(boxedExpression, index)
        }

        override fun getUnboxingExpressions(boxedText: String): Map<OutputValue, String> {
            return when (outputValues.size) {
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

        override val boxingRequired: Boolean = outputValues.size > 0

        override fun getBoxingExpressionPattern(arguments: List<KtExpression>): String? {
            if (arguments.isEmpty()) return null
            return arguments.indices.joinToString(prefix = "kotlin.collections.listOf(", separator = ", ", postfix = ")") { "\$$it" }
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
        if (size > 1) throw IllegalArgumentException("Multiple expression values: ${outputValues.joinToString()}") else firstOrNull()
    }

    val jumpOutputValue: Jump? = with(outputValues.filterIsInstance<Jump>()) {
        val jumpCount = size
        when {
            isEmpty() ->
                null
            outputValues.size > jumpCount || jumpCount > 1 ->
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
            returnType.nullability() != TypeNullability.FLEXIBLE ->
                listOf(returnType.approximateFlexibleTypes())
            else ->
                (returnType.unwrap() as FlexibleType).let { listOf(it.upperBound, it.lowerBound) }
        }
    }

fun ControlFlow.toDefault(): ControlFlow =
        copy(outputValues = outputValues.filterNot { it is Jump || it is ExpressionValue })

fun ControlFlow.copy(oldToNewParameters: Map<Parameter, Parameter>): ControlFlow {
    val newOutputValues = outputValues.map {
        if (it is ParameterUpdate) ParameterUpdate(oldToNewParameters[it.parameter]!!, it.originalExpressions) else it
    }
    return copy(outputValues = newOutputValues)
}

data class ExtractableCodeDescriptor(
        val extractionData: ExtractionData,
        val originalContext: BindingContext,
        val suggestedNames: List<String>,
        val visibility: KtModifierKeywordToken?,
        val parameters: List<Parameter>,
        val receiverParameter: Parameter?,
        val typeParameters: List<TypeParameter>,
        val replacementMap: MultiMap<KtSimpleNameExpression, Replacement>,
        val controlFlow: ControlFlow,
        val returnType: KotlinType,
        val modifiers: List<KtKeywordToken> = emptyList()
) {
    val name: String get() = suggestedNames.firstOrNull() ?: ""
    val duplicates: List<DuplicateInfo> by lazy { findDuplicates() }
}

fun ExtractableCodeDescriptor.copy(
        newName: String,
        newVisibility: KtModifierKeywordToken?,
        oldToNewParameters: Map<Parameter, Parameter>,
        newReceiver: Parameter?,
        returnType: KotlinType?
): ExtractableCodeDescriptor {
    val newReplacementMap = MultiMap.create<KtSimpleNameExpression, Replacement>()
    for ((ref, replacements) in replacementMap.entrySet()) {
        val newReplacements = replacements.map {
            if (it is ParameterReplacement) {
                val parameter = it.parameter
                val newParameter = oldToNewParameters[parameter] ?: return@map it
                it.copy(newParameter)
            }
            else it
        }
        newReplacementMap.putValues(ref, newReplacements)
    }

    return ExtractableCodeDescriptor(
            extractionData,
            originalContext,
            listOf(newName),
            newVisibility,
            oldToNewParameters.values.filter { it != newReceiver },
            newReceiver,
            typeParameters,
            newReplacementMap,
            controlFlow.copy(oldToNewParameters),
            returnType ?: this.returnType,
            modifiers)
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
            val expression = descriptor.extractionData.expressions.singleOrNull()
            return expression != null && expression !is KtDeclaration && expression !is KtBlockExpression
        }

        fun checkSimpleControlFlow(descriptor: ExtractableCodeDescriptor): Boolean {
            val outputValue = descriptor.controlFlow.outputValues.singleOrNull()
            return (outputValue is ExpressionValue && !outputValue.callSiteReturn) || outputValue is Initializer
        }

        fun checkSignatureAndParent(descriptor: ExtractableCodeDescriptor): Boolean {
            if (!descriptor.parameters.isEmpty()) return false
            if (descriptor.returnType.isUnit()) return false

            val parent = descriptor.extractionData.targetSibling.parent
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
        val dummyName: String? = null,
        val allowExpressionBody: Boolean = true,
        val delayInitialOccurrenceReplacement: Boolean = false
) {
    companion object {
        @JvmField val DEFAULT = ExtractionGeneratorOptions()
    }
}

data class ExtractionGeneratorConfiguration(
        val descriptor: ExtractableCodeDescriptor,
        val generatorOptions: ExtractionGeneratorOptions
)

data class ExtractionResult(
        val config: ExtractionGeneratorConfiguration,
        val declaration: KtNamedDeclaration,
        val duplicateReplacers: Map<KotlinPsiRange, () -> Unit>
) : Disposable {
    override fun dispose() = unmarkReferencesInside(declaration)
}

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
        SYNTAX_ERRORS,
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
                        SYNTAX_ERRORS -> "cannot.refactor.syntax.errors"
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

            return additionalInfo?.let { "$message\n\n${it.joinToString("\n") { StringUtil.htmlEmphasize(it) }}" } ?: message
        }
    }
}

class ExtractableCodeDescriptorWithConflicts(
        val descriptor: ExtractableCodeDescriptor,
        val conflicts: MultiMap<PsiElement, String>
)
