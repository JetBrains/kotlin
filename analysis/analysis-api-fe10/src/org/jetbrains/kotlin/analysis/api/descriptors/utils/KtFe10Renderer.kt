/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.utils

import org.jetbrains.kotlin.analysis.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.analysis.api.components.RendererModifier
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.classId
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.isExplicitOverride
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktModality
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtConstantValueRenderer
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.kotlin.types.typeUtil.isNullableAny
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal typealias KtFe10RendererConsumer = StringBuilder

internal class KtFe10Renderer(
    private val analysisContext: Fe10AnalysisContext,
    private val options: KtDeclarationRendererOptions,
    isDebugText: Boolean = false
) {
    private companion object {
        private val INDENTATION = " ".repeat(4)

        val IGNORED_VISIBILITIES: Set<Visibility> = setOf(
            Visibilities.Local,
            Visibilities.PrivateToThis,
            Visibilities.InvisibleFake,
            Visibilities.Inherited,
            Visibilities.Unknown,
            Visibilities.DEFAULT_VISIBILITY
        )

        val IGNORED_SUPERTYPES: Set<ClassId> = setOf(
            StandardClassIds.Enum,
            StandardClassIds.Annotation
        )
    }

    private val typeRenderer = KtFe10TypeRenderer(options.typeRendererOptions, isDebugText)

    private val typeApproximator = TypeApproximator(
        analysisContext.builtIns,
        analysisContext.resolveSession.languageVersionSettings
    )

    private var indentation = 0

    private fun KtFe10RendererConsumer.curlyBlock(body: KtFe10RendererConsumer.() -> Unit) {
        append(" {")
        indentedBlock(body)
        appendLine()
        renderIndentation()
        append('}')
    }

    private fun KtFe10RendererConsumer.indentedBlock(body: KtFe10RendererConsumer.() -> Unit) {
        indentation += 1
        body()
        indentation -= 1
    }

    fun render(descriptor: DeclarationDescriptor, consumer: KtFe10RendererConsumer) {
        consumer.renderDeclaration(descriptor)
    }

    private fun KtFe10RendererConsumer.renderType(type: KotlinType, shouldApproximate: Boolean = false) {
        if (shouldApproximate) {
            val approximatedType = typeApproximator.approximateToSuperType(type.unwrap(), PublicApproximatorConfiguration)
                ?: type.takeIf { it.constructor.declarationDescriptor?.name != SpecialNames.NO_NAME_PROVIDED }
                ?: analysisContext.builtIns.anyType

            renderType(approximatedType, shouldApproximate = false)
            return
        }

        typeRenderer.render(type, this)
    }

    private fun needsParenthesis(type: KotlinType): Boolean {
        if (!type.annotations.isEmpty()) {
            return true
        }

        if (options.typeRendererOptions.renderFunctionType && type.isFunctionType) {
            return true
        }

        val descriptor = type.constructor.declarationDescriptor
        if (descriptor is FunctionClassDescriptor && descriptor.functionKind.isSuspendType) {
            return true
        }

        return false
    }

    private fun KtFe10RendererConsumer.renderDeclaration(descriptor: DeclarationDescriptor) {
        when (descriptor) {
            is ClassifierDescriptor -> renderClassifier(descriptor)
            is CallableDescriptor -> renderCallable(descriptor)
            else -> error("Unexpected descriptor kind: $descriptor")
        }
    }

    private fun KtFe10RendererConsumer.renderClassifier(descriptor: ClassifierDescriptor) {
        when (descriptor) {
            is TypeAliasDescriptor -> renderTypeAlias(descriptor)
            is TypeParameterDescriptor -> renderTypeParameter(descriptor)
            is ClassDescriptor -> renderClass(descriptor)
            else -> error("Unexpected descriptor kind: $descriptor")
        }
    }

    private fun KtFe10RendererConsumer.renderTypeAlias(descriptor: TypeAliasDescriptor) {
        if (options.renderDeclarationHeader) {
            renderAnnotations(descriptor.annotations)
            renderModifiers(descriptor)
            append("typealias ")
        }
        renderName(descriptor)
        renderTypeParameters(descriptor.declaredTypeParameters)
        append(" = ")
        renderType(descriptor.expandedType)
    }

    private fun KtFe10RendererConsumer.renderTypeParameters(typeParameters: List<TypeParameterDescriptor>) {
        renderList(typeParameters, separator = ", ", prefix = "<", postfix = ">", renderWhenEmpty = false) { renderTypeParameter(it) }
    }

    private fun KtFe10RendererConsumer.renderTypeParameter(descriptor: TypeParameterDescriptor) {
        renderModifier("reified", descriptor.isReified)

        val variance = descriptor.variance.label
        renderModifier(variance, variance.isNotEmpty())

        renderAnnotations(descriptor.annotations)
        renderName(descriptor)

        val upperBounds = descriptor.upperBounds.filterNot { it.isNullableAny() }
        renderList(upperBounds, separator = " & ", prefix = " : ", postfix = "", renderWhenEmpty = false) { renderType(it) }
    }

    private fun KtFe10RendererConsumer.renderClass(descriptor: ClassDescriptor) {
        appendLine()
        renderIndentation()

        if (options.renderDeclarationHeader) {
            renderAnnotations(descriptor.annotations)
            renderModifiers(descriptor)


            if (DescriptorUtils.isAnonymousObject(descriptor)) {
                append("object")
            } else {
                val classKeyword = when (descriptor.kind) {
                    ClassKind.CLASS -> "class"
                    ClassKind.INTERFACE -> "interface"
                    ClassKind.ENUM_CLASS -> "enum class"
                    ClassKind.ENUM_ENTRY -> "enum entry"
                    ClassKind.ANNOTATION_CLASS -> "annotation class"
                    ClassKind.OBJECT -> "object"
                }
                append(classKeyword)
            }
        }

        val shouldRenderName = !descriptor.name.isSpecial
                && (!descriptor.isCompanionObject || descriptor.name != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)

        if (shouldRenderName) {
            append(' ')
            renderName(descriptor)
        }

        renderTypeParameters(descriptor.declaredTypeParameters)
        renderSupertypes(descriptor)

        if (options.renderContainingDeclarations) {
            val (enumEntries, otherDeclarations) = descriptor.unsubstitutedMemberScope.getContributedDescriptors()
                .filter { shouldRenderNestedDeclaration(descriptor, it) }
                .partition { it is ClassDescriptor && it.kind == ClassKind.ENUM_ENTRY }

            val constructors = descriptor.constructors
                .filter { shouldRenderNestedDeclaration(descriptor, it) }

            if (enumEntries.isNotEmpty() || otherDeclarations.isNotEmpty() || constructors.isNotEmpty()) {
                curlyBlock {
                    enumEntries.forEach { renderEnumEntry(it as ClassDescriptor) }
                    sortDeclarations(constructors).forEach { renderConstructor(it) }
                    sortDeclarations(otherDeclarations).forEach { renderDeclaration(it) }
                }
            }
        }
    }

    private fun shouldRenderNestedDeclaration(owner: ClassDescriptor, declaration: DeclarationDescriptor): Boolean {
        if (declaration is CallableMemberDescriptor && declaration.kind != CallableMemberDescriptor.Kind.DECLARATION) {
            return false
        }

        if (
            declaration is ConstructorDescriptor && !DescriptorUtils.isAnonymousObject(declaration.constructedClass)
            && declaration.isPrimary && declaration.valueParameters.isEmpty() && declaration.annotations.isEmpty()
        ) {
            if (declaration.visibility == DescriptorVisibilities.DEFAULT_VISIBILITY) {
                return false
            } else if (owner.kind == ClassKind.OBJECT || owner.kind == ClassKind.ENUM_CLASS) {
                return false
            }
        }

        if (declaration is FunctionDescriptor && owner.kind == ClassKind.ENUM_CLASS) {
            if (declaration.name.asString() == "valueOf" && KotlinBuiltIns.isString(declaration.valueParameters.singleOrNull()?.type)) {
                return false
            } else if (declaration.name.asString() == "values" && declaration.valueParameters.isEmpty()) {
                return false
            }
        }

        return true
    }

    private fun <T : DeclarationDescriptor> sortDeclarations(declarations: List<T>): List<T> {
        if (!options.sortNestedDeclarations) {
            return declarations
        }

        fun getDeclarationKind(declaration: DeclarationDescriptor): Int = when (declaration) {
            is ConstructorDescriptor -> if (declaration.isPrimary) 1 else 2
            is PropertyDescriptor -> 3
            is FunctionDescriptor -> 4
            else -> 5
        }

        return declarations.sortedWith(Comparator { left, right ->
            val kindResult = getDeclarationKind(left) - getDeclarationKind(right)
            if (kindResult != 0) {
                return@Comparator kindResult
            }

            val nameResult = left.name.asString().compareTo(right.name.asString())
            if (nameResult != 0) {
                return@Comparator nameResult
            }

            val leftString = buildString { renderDeclaration(left) }
            val rightString = buildString { renderDeclaration(right) }
            return@Comparator leftString.compareTo(rightString)
        })
    }

    private fun KtFe10RendererConsumer.renderSupertypes(descriptor: ClassDescriptor) {
        val allowedSuperClasses = (listOfNotNull(descriptor.getSuperClassNotAny()) + descriptor.getSuperInterfaces())
            .filterTo(HashSet()) { it.classId !in IGNORED_SUPERTYPES }

        val supertypes = descriptor.typeConstructor.supertypes.filter { it.constructor.declarationDescriptor in allowedSuperClasses }
        renderList(supertypes, separator = ", ", prefix = " : ", postfix = "", renderWhenEmpty = false) { renderType(it) }
    }

    private fun KtFe10RendererConsumer.renderCallable(descriptor: CallableDescriptor) {
        when (descriptor) {
            is PropertyGetterDescriptor -> renderPropertyAccessor(descriptor)
            is PropertySetterDescriptor -> renderPropertyAccessor(descriptor)
            is PropertyDescriptor -> renderProperty(descriptor)
            is ConstructorDescriptor -> renderConstructor(descriptor)
            is FunctionDescriptor -> renderFunction(descriptor)
            is ValueParameterDescriptor -> renderValueParameter(descriptor)
            is LocalVariableDescriptor -> renderLocalVariable(descriptor)
            else -> error("Unexpected descriptor kind: $descriptor")
        }
    }

    private fun KtFe10RendererConsumer.renderPropertyAccessor(descriptor: PropertyAccessorDescriptor) {
        appendLine()
        indentedBlock {
            renderIndentation()
            if (options.renderDeclarationHeader) {
                renderAnnotations(descriptor.annotations)
                renderModifiers(descriptor)
            }

            when (descriptor) {
                is PropertyGetterDescriptor -> {
                    append("get()")
                }
                is PropertySetterDescriptor -> {
                    append("set(")
                    val valueParameter = descriptor.valueParameters.singleOrNull()
                    if (valueParameter != null) {
                        val name = valueParameter.name.takeIf { !it.isSpecial } ?: Name.identifier("value")
                        renderValueParameter(valueParameter, name)
                    }
                    append(")")
                }
            }
        }
    }

    private fun KtFe10RendererConsumer.renderEnumEntry(descriptor: ClassDescriptor) {
        assert(descriptor.kind == ClassKind.ENUM_ENTRY)

        appendLine()
        renderIndentation()

        renderName(descriptor)
        append(',')
    }

    private fun KtFe10RendererConsumer.renderLocalVariable(descriptor: LocalVariableDescriptor) {
        appendLine()
        renderIndentation()

        if (options.renderDeclarationHeader) {
            renderAnnotations(descriptor.annotations)
            append(if (descriptor.isVar) "var" else "val").append(' ')
        }

        renderName(descriptor)
        append(": ")
        renderType(descriptor.type, shouldApproximate = options.approximateTypes)
    }

    private fun KtFe10RendererConsumer.renderProperty(descriptor: PropertyDescriptor) {
        appendLine()
        renderIndentation()

        if (options.renderDeclarationHeader) {
            renderAnnotations(descriptor.annotations)
            renderModifiers(descriptor)
            append(if (descriptor.isVar) "var" else "val").append(' ')
            renderTypeParameters(descriptor.typeParameters)
            if (descriptor.typeParameters.isNotEmpty()) {
                append(' ')
            }
        }

        renderReceiver(descriptor)
        renderName(descriptor)
        append(": ")
        renderType(descriptor.type, shouldApproximate = options.approximateTypes)

        if (options.renderContainingDeclarations) {
            fun shouldRenderAccessor(accessor: PropertyAccessorDescriptor): Boolean {
                return descriptor.isDelegated
                        || accessor.hasBody()
                        || !accessor.annotations.isEmpty()
                        || accessor.visibility != descriptor.visibility
            }

            val getter = descriptor.getter
            val setter = descriptor.setter

            val shouldRenderAccessors = (getter != null && shouldRenderAccessor(getter)) || (setter != null && shouldRenderAccessor(setter))
            if (shouldRenderAccessors) {
                if (getter != null) {
                    renderPropertyAccessor(getter)
                }
                if (setter != null) {
                    renderPropertyAccessor(setter)
                }
            }
        }
    }

    private fun KtFe10RendererConsumer.renderReceiver(descriptor: CallableDescriptor) {
        val extensionReceiver = descriptor.extensionReceiverParameter ?: return
        val needsParentheses = !descriptor.annotations.isEmpty() || needsParenthesis(extensionReceiver.type)
        if (needsParentheses) {
            append('(')
        }
        renderType(extensionReceiver.type)
        if (needsParentheses) {
            append(')')
        }
        append('.')
    }

    private fun KtFe10RendererConsumer.renderConstructor(descriptor: ConstructorDescriptor) {
        appendLine()
        renderIndentation()
        if (options.renderDeclarationHeader) {
            renderAnnotations(descriptor.annotations)
        }
        append("constructor")
        renderValueParameters(descriptor.valueParameters)

        renderLocalDeclarations(descriptor)
    }

    private fun KtFe10RendererConsumer.renderFunction(descriptor: FunctionDescriptor) {
        appendLine()
        renderIndentation()

        if (options.renderDeclarationHeader) {
            renderAnnotations(descriptor.annotations)
            renderModifiers(descriptor)
            append("fun ")
            renderTypeParameters(descriptor.typeParameters)
            if (descriptor.typeParameters.isNotEmpty()) {
                append(" ")
            }
        }

        renderReceiver(descriptor)
        renderName(descriptor)
        renderValueParameters(descriptor.valueParameters)

        val returnType = descriptor.returnType
        if (returnType != null && !returnType.isUnit()) {
            append(": ")
            renderType(returnType, shouldApproximate = options.approximateTypes)
        }

        renderLocalDeclarations(descriptor)
    }

    private fun KtFe10RendererConsumer.renderValueParameters(valueParameters: List<ValueParameterDescriptor>) {
        renderList(valueParameters, separator = ", ", prefix = "(", postfix = ")", renderWhenEmpty = true) { renderValueParameter(it) }
    }

    private fun KtFe10RendererConsumer.renderValueParameter(descriptor: ValueParameterDescriptor, name: Name = descriptor.name) {
        if (options.renderDeclarationHeader) {
            renderAnnotations(descriptor.annotations)
        }

        renderModifiers(descriptor)
        append(name.render())
        append(": ")
        renderType(descriptor.varargElementType ?: descriptor.type)

        if (options.renderDefaultParameterValue && descriptor.hasDefaultValue()) {
            append(" = ...")
        }
    }

    private fun KtFe10RendererConsumer.renderLocalDeclarations(descriptor: CallableMemberDescriptor) {
        if (!options.renderContainingDeclarations) {
            return
        }

        val body = when (val declaration = descriptor.source.getPsi()) {
            is KtFunction -> declaration.bodyExpression
            is KtPropertyAccessor -> declaration.bodyExpression
            else -> null
        }

        val collectedChildren = mutableListOf<DeclarationDescriptor>()

        body?.accept(object : KtTreeVisitorVoid() {
            override fun visitDeclaration(declaration: KtDeclaration) {
                if (declaration is KtFunction || declaration is KtVariableDeclaration || declaration is KtClassOrObject) {
                    val bindingContext = analysisContext.analyze(declaration)
                    val childDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
                    if (childDescriptor != null) {
                        collectedChildren += childDescriptor
                    }
                } else {
                    super.visitDeclaration(declaration)
                }
            }
        })

        if (collectedChildren.isNotEmpty()) {
            curlyBlock {
                collectedChildren.forEach { renderDeclaration(it) }
            }
        }
    }

    private fun KtFe10RendererConsumer.renderName(descriptor: DeclarationDescriptor) {
        append(descriptor.name.render())
    }

    private fun KtFe10RendererConsumer.renderAnnotations(annotations: Annotations, predicate: (ClassId) -> Boolean = { true }) {
        if (RendererModifier.ANNOTATIONS !in options.modifiers) {
            return
        }

        renderFe10Annotations(annotations, predicate)
    }

    private fun KtFe10RendererConsumer.renderModifiers(descriptor: DeclarationDescriptor) {
        if (descriptor is MemberDescriptor) {
            renderVisibility(descriptor)
        }

        if (descriptor is PropertyDescriptor) {
            renderModifier("const", descriptor.isConst, RendererModifier.CONST)
            renderModifier("lateinit", descriptor.isLateInit, RendererModifier.LATEINIT)
        }

        if (descriptor is ValueParameterDescriptor) {
            renderModifier("crossinline", descriptor.isCrossinline)
            renderModifier("noinline", descriptor.isNoinline)
        }

        if (descriptor is MemberDescriptor) {
            renderModifier("external", descriptor.isExternal)
            renderModifier("expect", descriptor.isExpect, RendererModifier.EXPECT)
            renderModifier("actual", descriptor.isActual, RendererModifier.ACTUAL)
            renderModality(descriptor)
        }

        if (descriptor is CallableMemberDescriptor) {
            renderModifier("override", descriptor.isExplicitOverride, RendererModifier.OVERRIDE)
        }

        if (descriptor is ValueParameterDescriptor) {
            renderModifier("vararg", descriptor.isVararg)
        }

        if (descriptor is FunctionDescriptor) {
            renderModifier("tailrec", descriptor.isTailrec)
            renderModifier("suspend", descriptor.isSuspend)
            renderModifier("inline", descriptor.isInline, RendererModifier.INLINE)
            renderModifier("infix", descriptor.isInfix)
            renderModifier("operator", descriptor.isOperator, RendererModifier.OPERATOR)
        }

        if (descriptor is ClassDescriptor) {
            renderModifier("inner", descriptor.isInner, RendererModifier.INNER)
            renderModifier("data", descriptor.isData, RendererModifier.DATA)
            renderModifier("inline", descriptor.isInline, RendererModifier.INNER)
            renderModifier("fun", descriptor.isFun, RendererModifier.FUN)
            renderModifier("companion", descriptor.isCompanionObject)
        }
    }

    private fun KtFe10RendererConsumer.renderVisibility(descriptor: DeclarationDescriptorWithVisibility) {
        val visibility = descriptor.ktVisibility
        renderModifier(visibility.internalDisplayName, visibility !in IGNORED_VISIBILITIES, RendererModifier.VISIBILITY)
    }

    private fun KtFe10RendererConsumer.renderModality(descriptor: MemberDescriptor) {
        if (descriptor is PropertyAccessorDescriptor || (descriptor is CallableMemberDescriptor && descriptor.isExplicitOverride)) {
            return
        }

        val modality = descriptor.ktModality

        renderModifier(
            modality.name.toLowerCaseAsciiOnly(),
            modality != getDefaultModality(descriptor),
            RendererModifier.MODALITY
        )
    }

    private fun getDefaultModality(descriptor: MemberDescriptor): Modality {
        when (descriptor) {
            is ClassDescriptor -> return if (descriptor.kind == ClassKind.INTERFACE) Modality.ABSTRACT else Modality.FINAL
            is CallableMemberDescriptor -> {
                val containingDeclaration = descriptor.containingDeclaration
                if (containingDeclaration !is ClassDescriptor) {
                    return Modality.FINAL
                }
                if (descriptor.isExplicitOverride && containingDeclaration.ktModality != Modality.FINAL) {
                    return Modality.OPEN
                } else if (containingDeclaration.kind == ClassKind.INTERFACE && descriptor.visibility != DescriptorVisibilities.PRIVATE) {
                    return if (descriptor.ktModality == Modality.ABSTRACT) Modality.ABSTRACT else Modality.OPEN
                }

                return Modality.FINAL
            }
            else -> return Modality.FINAL
        }
    }

    private fun KtFe10RendererConsumer.renderModifier(text: String, state: Boolean, modifier: RendererModifier? = null) {
        if (state && (modifier == null || modifier in options.modifiers)) {
            append(text).append(' ')
        }
    }

    private fun KtFe10RendererConsumer.renderIndentation() {
        append(INDENTATION.repeat(indentation))
    }
}

internal fun <T> KtFe10RendererConsumer.renderList(
    list: Collection<T>,
    separator: String,
    prefix: String = "",
    postfix: String = "",
    renderWhenEmpty: Boolean = true,
    renderItem: KtFe10RendererConsumer.(T) -> Unit
) {
    if (!renderWhenEmpty && list.isEmpty()) {
        return
    }

    append(prefix)
    list.forEachIndexed { index, item ->
        if (index > 0) {
            append(separator)
        }
        renderItem(item)
    }
    append(postfix)
}