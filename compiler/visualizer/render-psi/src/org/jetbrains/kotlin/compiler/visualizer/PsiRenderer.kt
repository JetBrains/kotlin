/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.visualizer

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.compiler.visualizer.Annotator.annotate
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.declaresOrInheritsDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import java.util.ArrayList

class PsiRenderer(private val file: KtFile, analysisResult: AnalysisResult) : BaseRenderer {
    val bindingContext = analysisResult.bindingContext
    private val annotations = mutableListOf<Annotator.AnnotationInfo>()

    val descriptorRenderer = PsiDescriptorRenderer()

    private val unnecessaryData = mapOf(
        "kotlin." to "",
        "kotlin/" to ""
    )

    private fun addAnnotation(annotationText: String, element: PsiElement, deleteDuplicate: Boolean = true) {
        annotations.removeIf { it.range.startOffset == element.textRange.startOffset && deleteDuplicate }

        var textWithOutUnnecessaryData = annotationText
        for ((key, value) in unnecessaryData) {
            textWithOutUnnecessaryData = textWithOutUnnecessaryData.replace(key, value)
        }
        if (textWithOutUnnecessaryData != element.text && textWithOutUnnecessaryData.isNotEmpty()) {
            annotations.add(Annotator.AnnotationInfo(textWithOutUnnecessaryData, element.textRange))
        }
    }

    override fun render(): String {
        file.accept(Renderer())
        return annotate(file.text, annotations).joinToString("\n")
    }

    inner class Renderer : KtVisitorVoid() {
        private val implicitReceivers = mutableListOf<ReceiverValue>()
        private var lastCallWithLambda = ""

        private fun renderType(type: KotlinType?): String {
            return type?.let { descriptorRenderer.renderType(it) } ?: "[ERROR: unknown type]"
        }

        private fun renderType(descriptor: CallableDescriptor?): String {
            return renderType(descriptor?.returnType)
        }

        private fun renderType(expression: KtExpression?): String {
            return renderType(expression?.let { bindingContext.getType(it) })
        }

        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        override fun visitKtElement(element: KtElement) {
            element.acceptChildren(this)
        }

        override fun visitPackageDirective(directive: KtPackageDirective) {
            //don't resolve package names
        }

        override fun visitSuperExpression(expression: KtSuperExpression) {
            //don't resolve super expression
        }

        override fun visitThisExpression(expression: KtThisExpression) {
            //don't resolve this expression
        }

        private fun renderVariableType(variable: KtVariableDeclaration) {
            val descriptor = bindingContext[VARIABLE, variable]
            addAnnotation(renderType(descriptor), variable.nameIdentifier!!)
            variable.acceptChildren(this)
        }

        override fun visitProperty(property: KtProperty) =
            renderVariableType(property)

        override fun visitDestructuringDeclarationEntry(multiDeclarationEntry: KtDestructuringDeclarationEntry) =
            renderVariableType(multiDeclarationEntry)

        override fun visitTypeReference(typeReference: KtTypeReference) {
            if (typeReference.text.isNotEmpty()) {
                val hasResolvedCall = with(object : KtVisitorVoid() {
                    var hasCall: Boolean = false
                    override fun visitKtElement(element: KtElement) {
                        if (!hasCall) {
                            hasCall = element.getResolvedCall(bindingContext) != null
                            element.acceptChildren(this)
                        }
                    }
                }) {
                    typeReference.accept(this)
                    this.hasCall
                }

                if (!hasResolvedCall) {
                    val type = typeReference.getAbbreviatedTypeOrType(bindingContext)
                    addAnnotation(renderType(type), typeReference)
                }
            }
            super.visitTypeReference(typeReference)
        }

        override fun visitConstantExpression(expression: KtConstantExpression) {
            addAnnotation(renderType(expression), expression)
        }

        private fun renderCall(expression: KtExpression): ResolvedCall<out CallableDescriptor>? {
            val call = expression.getCall(bindingContext)
            val resolvedCall = expression.getResolvedCall(bindingContext)
            if (call == null) {
                return null
            } else if (resolvedCall == null) {
                addAnnotation("[ERROR: not resolved]", expression)
                return null
            }

            val descriptor = resolvedCall.resultingDescriptor
            val annotation = descriptorRenderer.render(descriptor)
            addAnnotation(annotation, expression, deleteDuplicate = false)

            fun addReceiverAnnotation(receiver: ReceiverValue?, receiverKind: ExplicitReceiverKind) {
                if (receiver != null && resolvedCall.explicitReceiverKind != receiverKind) {
                    val index = implicitReceivers.indexOf(receiver)
                    if (index != -1) {
                        addAnnotation("this@$index", expression, deleteDuplicate = false)
                    }
                }
            }

            addReceiverAnnotation(resolvedCall.extensionReceiver, ExplicitReceiverKind.EXTENSION_RECEIVER)
            addReceiverAnnotation(resolvedCall.dispatchReceiver, ExplicitReceiverKind.DISPATCH_RECEIVER)

            return resolvedCall
        }

        override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
            val qualifierDescriptor = bindingContext[QUALIFIER, expression]?.descriptor
            if (qualifierDescriptor != null) {
                addAnnotation(descriptorRenderer.render(qualifierDescriptor), expression)
            } else {
                renderCall(expression)
            }
        }

        override fun visitIfExpression(expression: KtIfExpression) {
            addAnnotation(renderType(expression), expression.ifKeyword)
            super.visitIfExpression(expression)
        }

        override fun visitWhenExpression(expression: KtWhenExpression) {
            addAnnotation(renderType(expression), expression.whenKeyword)
            super.visitWhenExpression(expression)
        }

        override fun visitWhenEntry(jetWhenEntry: KtWhenEntry) {
            addAnnotation(renderType(jetWhenEntry.expression), jetWhenEntry.expression!!)
            super.visitWhenEntry(jetWhenEntry)
        }

        override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
            val descriptor = bindingContext[DECLARATION_TO_DESCRIPTOR, lambdaExpression.functionLiteral] as AnonymousFunctionDescriptor
            val extensionReceiver = descriptor.extensionReceiverParameter ?: return super.visitLambdaExpression(lambdaExpression)
            addAnnotation("$lastCallWithLambda@${implicitReceivers.size}", lambdaExpression)

            implicitReceivers += extensionReceiver.value
            super.visitLambdaExpression(lambdaExpression)
            implicitReceivers -= extensionReceiver.value
        }

        override fun visitCallExpression(expression: KtCallExpression) {
            val resolvedCall = renderCall(expression) ?: return super.visitCallExpression(expression)

            if (expression.getChildOfType<KtLambdaArgument>() != null) {
                lastCallWithLambda = resolvedCall.resultingDescriptor.name.asString()
            }
            for (child in expression.children) {
                if (child.node.elementType != KtNodeTypes.REFERENCE_EXPRESSION) {
                    child.accept(this)
                }
            }
        }
    }

    inner class PsiDescriptorRenderer : DeclarationDescriptorVisitor<Unit, StringBuilder> {
        private val typeRenderer: DescriptorRenderer = DescriptorRenderer.withOptions {
            withDefinedIn = false
            modifiers = emptySet()
            classifierNamePolicy = /*ClassifierNamePolicy.SOURCE_CODE_QUALIFIED*/object : ClassifierNamePolicy {
                override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
                    return (if (classifier is TypeParameterDescriptor) renderer.renderName(classifier.name, false)
                    else renderer.renderFqName(DescriptorUtils.getFqName(classifier))).replace(".", "/")
                }
            }
            includeAdditionalModifiers = false
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE

            withoutTypeParameters = true
        }

        private fun CallableDescriptor.isSpecial(): Boolean {
            return this.name.asString().contains("SPECIAL-FUNCTION")
        }

        fun render(declarationDescriptor: DeclarationDescriptor): String {
            if (declarationDescriptor is CallableDescriptor && declarationDescriptor.isSpecial()) {
                return this.renderSpecialFunction(declarationDescriptor)
            }
            return buildString {
                declarationDescriptor.accept(this@PsiDescriptorRenderer, this)
            }
        }

        fun renderType(type: KotlinType): String {
            return typeRenderer.renderType(type)
        }

        private fun renderName(descriptor: DeclarationDescriptor, hasReceiver: Boolean = false): String {
            return if (hasReceiver) {
                descriptor.name.asString()
            } else {
                renderFqName(descriptor)
            }
        }

        private fun renderFqName(descriptor: DeclarationDescriptor): String {
            /*generateSequence(descriptor) { it.containingDeclaration }
                .takeWhile {  }*/
            return descriptor.fqNameUnsafe.render().let {
                if (descriptor !is PropertyDescriptor && descriptor !is VariableDescriptor)
                    it.replace(".", "/")
                else it
            }
        }

        private fun renderReceiver(descriptor: CallableDescriptor, data: StringBuilder): ReceiverParameterDescriptor? {
            return descriptor.extensionReceiverParameter?.also {
                visitReceiverParameterDescriptor(it, data)
                data.append(".")
            } ?: descriptor.dispatchReceiverParameter?.also {
                data.append("(")
                visitReceiverParameterDescriptor(it, data)
                data.append(").")
            }
        }

        private fun renderSuperTypes(klass: ClassDescriptor, builder: StringBuilder) {
            if (KotlinBuiltIns.isNothing(klass.defaultType)) return

            val supertypes = klass.typeConstructor.supertypes
            if (supertypes.isEmpty() || supertypes.size == 1 && KotlinBuiltIns.isAnyOrNullableAny(supertypes.iterator().next())) return

            builder.append(": ")
            supertypes.joinTo(builder, ", ") { renderType(it) }
        }

        private fun renderValueParameter(
            parameter: ValueParameterDescriptor, includeNames: Boolean, data: StringBuilder, topLevel: Boolean
        ) {
            renderVariable(parameter, includeNames, data, topLevel)
            if (parameter.declaresOrInheritsDefaultValue()) {
                data.append(" = ...")
            }
        }

        private fun renderVariable(variable: VariableDescriptor, includeName: Boolean, data: StringBuilder, topLevel: Boolean) {
            val realType = variable.type

            val varargElementType = (variable as? ValueParameterDescriptor)?.varargElementType
            val typeToRender = varargElementType ?: realType
            if (varargElementType != null) data.append("vararg ")

            if (topLevel && variable !is ValueParameterDescriptor) data.append(if (variable.isVar) "var" else "val").append(" ")
            if (includeName) {
                data.append(renderName(variable)).append(": ")
            }

            data.append(renderType(typeToRender))
        }

        private fun renderSpecialFunction(descriptor: CallableDescriptor): String {
            val descriptorName = descriptor.name.asString()
            val name = when {
                descriptorName.contains("ELVIS") -> "?:"
                descriptorName.contains("EXCLEXCL") -> "!!"
                else -> "UNKNOWN"
            }
            val valueParameters = buildString { visitValueParameters(descriptor.valueParameters, this) }
            val returnType = descriptor.returnType?.let { renderType(it) } ?: "[ERROR: unknown type]"

            return "fun $name $valueParameters: $returnType"
        }

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: StringBuilder) {
            data.append("package-fragment ${renderFqName(descriptor)}")
        }

        override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: StringBuilder) {
            data.append("package ${renderFqName(descriptor)}")
        }

        override fun visitVariableDescriptor(variable: VariableDescriptor, data: StringBuilder) {
            renderVariable(variable, true, data, true)
        }

        override fun visitFunctionDescriptor(function: FunctionDescriptor, data: StringBuilder) {
            data.append("fun ")

            visitTypeParameters(function.typeParameters, data)
            if (function.typeParameters.isNotEmpty()) data.append(" ")

            //render receiver
            val receiver = renderReceiver(function, data)

            //render name
            data.append(renderName(function, receiver != null))

            //render value parameters
            visitValueParameters(function.valueParameters, data)

            //render return type
            val returnType = function.returnType
            data.append(": ").append(if (returnType == null) "[NULL]" else renderType(returnType))

            renderWhereSuffix(function.typeParameters, data)
        }

        private fun renderWhereSuffix(typeParameters: List<TypeParameterDescriptor>, data: StringBuilder) {
            val upperBoundStrings = ArrayList<String>(0)

            for (typeParameter in typeParameters) {
                typeParameter.upperBounds
                    .drop(1) // first parameter is rendered by renderTypeParameter
                    .mapTo(upperBoundStrings) { renderName(typeParameter) + " : " + renderType(it) }
            }

            if (upperBoundStrings.isNotEmpty()) {
                data.append(" where ")
                upperBoundStrings.joinTo(data, ", ")
            }
        }

        private fun visitTypeParameters(typeParameters: List<TypeParameterDescriptor>, data: StringBuilder) {
            if (typeParameters.isNotEmpty()) {
                data.append("<")
                val iterator = typeParameters.iterator()
                while (iterator.hasNext()) {
                    val typeParameterDescriptor = iterator.next()
                    visitTypeParameterDescriptor(typeParameterDescriptor, data)
                    if (iterator.hasNext()) {
                        data.append(", ")
                    }
                }
                data.append(">")
            }
        }

        override fun visitTypeParameterDescriptor(typeParameter: TypeParameterDescriptor, data: StringBuilder) {
            data.append(renderName(typeParameter, true))
            val upperBoundsCount = typeParameter.upperBounds.size
            if (upperBoundsCount >= 1) {
                val upperBound = typeParameter.upperBounds.iterator().next()
                if (!KotlinBuiltIns.isDefaultBound(upperBound)) {
                    data.append(" : ").append(renderType(upperBound))
                }
            }
        }

        override fun visitClassDescriptor(klass: ClassDescriptor, data: StringBuilder) {
            data.append(DescriptorRenderer.getClassifierKindPrefix(klass)).append(" ")

            //render name
            data.append(renderName(klass))

            if (klass.kind == ClassKind.ENUM_ENTRY) return

            visitTypeParameters(klass.declaredTypeParameters, data)

            renderSuperTypes(klass, data)
            renderWhereSuffix(klass.declaredTypeParameters, data)
        }

        override fun visitTypeAliasDescriptor(typeAlias: TypeAliasDescriptor, data: StringBuilder) {
            data.append("typealias").append(" ")
            data.append(renderName(typeAlias))

            visitTypeParameters(typeAlias.declaredTypeParameters, data)

            data.append(" = ").append(renderType(typeAlias.underlyingType))
        }

        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: StringBuilder) {
            data.append(renderName(descriptor))
        }

        override fun visitConstructorDescriptor(constructor: ConstructorDescriptor, data: StringBuilder) {
            data.append("constructor").append(" ")

            val classDescriptor = constructor.containingDeclaration

            data.append(renderName(classDescriptor))
            visitTypeParameters(constructor.typeParameters, data)

            visitValueParameters(constructor.valueParameters, data)

            renderWhereSuffix(constructor.typeParameters, data)
        }

        override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: StringBuilder) {
            visitClassDescriptor(scriptDescriptor, data)
        }

        override fun visitPropertyDescriptor(property: PropertyDescriptor, data: StringBuilder) {
            data.append(if (property.isVar) "var" else "val").append(" ")
            visitTypeParameters(property.typeParameters, data)
            if (property.typeParameters.isNotEmpty()) data.append(" ")

            //render receiver
            val receiver = renderReceiver(property, data)

            //render name
            data.append(renderName(property, receiver != null))

            //render return type
            data.append(": ").append(renderType(property.type))

            renderWhereSuffix(property.typeParameters, data)
        }

        private fun visitValueParameters(parameters: List<ValueParameterDescriptor>, data: StringBuilder) {
            data.append("(")
            for ((index, parameter) in parameters.withIndex()) {
                renderValueParameter(parameter, false, data, false)
                if (index != parameters.size - 1) {
                    data.append(", ")
                }
            }
            data.append(")")
        }

        override fun visitValueParameterDescriptor(parameter: ValueParameterDescriptor, data: StringBuilder) {
            renderValueParameter(parameter, true, data, true)
        }

        override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: StringBuilder) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: StringBuilder) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: StringBuilder) {
            data.append(renderType(descriptor.type))
        }
    }
}
