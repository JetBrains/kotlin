package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.renderer.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.Printer

class RenderIrElementWithDescriptorsVisitor : IrElementVisitor<String, Nothing?> {
    override fun visitElement(element: IrElement, data: Nothing?): String =
            "? ${element.javaClass.simpleName}"

    override fun visitDeclaration(declaration: IrDeclaration, data: Nothing?): String =
            "? ${declaration.javaClass.simpleName} ${declaration.descriptor.ref()}"

    override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): String =
            "MODULE_FRAGMENT ${declaration.descriptor}"

    override fun visitFile(declaration: IrFile, data: Nothing?): String =
            "FILE ${declaration.name}"

    override fun visitFunction(declaration: IrFunction, data: Nothing?): String =
            "FUN ${declaration.descriptor}"

    override fun visitConstructor(declaration: IrConstructor, data: Nothing?): String =
            "CONSTRUCTOR ${declaration.descriptor}"

    override fun visitProperty(declaration: IrProperty, data: Nothing?): String =
            "PROPERTY ${declaration.descriptor}"

    override fun visitField(declaration: IrField, data: Nothing?): String =
            "FIELD ${declaration.descriptor}"

    override fun visitClass(declaration: IrClass, data: Nothing?): String =
            "CLASS ${declaration.descriptor}"

    override fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?): String =
            "TYPEALIAS ${declaration.descriptor} type=${declaration.descriptor.underlyingType.render()}"

    override fun visitVariable(declaration: IrVariable, data: Nothing?): String =
            "VAR ${declaration.descriptor}"

    override fun visitEnumEntry(declaration: IrEnumEntry, data: Nothing?): String =
            "ENUM_ENTRY ${declaration.descriptor}"

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?): String =
            "ANONYMOUS_INITIALIZER ${declaration.descriptor}"

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): String =
            "LOCAL_DELEGATED_PROPERTY ${declaration.descriptor}"

    override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?): String =
            "EXPRESSION_BODY"

    override fun visitBlockBody(body: IrBlockBody, data: Nothing?): String =
            "BLOCK_BODY"

    override fun visitSyntheticBody(body: IrSyntheticBody, data: Nothing?): String =
            "SYNTHETIC_BODY kind=${body.kind}"

    override fun visitExpression(expression: IrExpression, data: Nothing?): String =
            "? ${expression.javaClass.simpleName} type=${expression.type.render()}"

    override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): String =
            "CONST ${expression.kind} type=${expression.type.render()} value='${expression.value}'"

    override fun visitVararg(expression: IrVararg, data: Nothing?): String =
            "VARARG type=${expression.type} varargElementType=${expression.varargElementType}"

    override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): String =
            "SPREAD_ELEMENT"

    override fun visitBlock(expression: IrBlock, data: Nothing?): String =
            "BLOCK type=${expression.type.render()} origin=${expression.origin}"

    override fun visitComposite(expression: IrComposite, data: Nothing?): String =
            "COMPOSITE type=${expression.type.render()} origin=${expression.origin}"

    override fun visitReturn(expression: IrReturn, data: Nothing?): String =
            "RETURN type=${expression.type.render()} from='${expression.returnTarget}'"

    override fun visitCall(expression: IrCall, data: Nothing?): String =
            "CALL '${expression.descriptor}' ${expression.renderSuperQualifier()}" +
                    "type=${expression.type.render()} origin=${expression.origin}"

    private fun IrCall.renderSuperQualifier(): String =
            superQualifier?.let { "superQualifier=${it.name} " } ?: ""

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?): String =
            "DELEGATING_CONSTRUCTOR_CALL '${expression.descriptor}'"

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?): String =
            "ENUM_CONSTRUCTOR_CALL '${expression.descriptor}'"

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: Nothing?): String =
            "INSTANCE_INITIALIZER_CALL classDescriptor='${expression.classDescriptor}'"

    override fun visitGetValue(expression: IrGetValue, data: Nothing?): String =
            "GET_VAR '${expression.descriptor}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitSetVariable(expression: IrSetVariable, data: Nothing?): String =
            "SET_VAR '${expression.descriptor}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitGetField(expression: IrGetField, data: Nothing?): String =
            "GET_FIELD '${expression.descriptor}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitSetField(expression: IrSetField, data: Nothing?): String =
            "SET_FIELD '${expression.descriptor}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Nothing?): String =
            "GET_OBJECT '${expression.descriptor}' type=${expression.type.render()}"

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: Nothing?): String =
            "GET_ENUM '${expression.descriptor}' type=${expression.type.render()}"

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): String =
            "STRING_CONCATENATION type=${expression.type.render()}"

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): String =
            "TYPE_OP origin=${expression.operator} typeOperand=${expression.typeOperand.render()}"

    override fun visitWhen(expression: IrWhen, data: Nothing?): String =
            "WHEN type=${expression.type.render()} origin=${expression.origin}"

    override fun visitBranch(branch: IrBranch, data: Nothing?): String =
            "BRANCH"

    override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?): String =
            "WHILE label=${loop.label} origin=${loop.origin}"

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Nothing?): String =
            "DO_WHILE label=${loop.label} origin=${loop.origin}"

    override fun visitBreak(jump: IrBreak, data: Nothing?): String =
            "BREAK label=${jump.label} loop.label=${jump.loop.label}"

    override fun visitContinue(jump: IrContinue, data: Nothing?): String =
            "CONTINUE label=${jump.label} loop.label=${jump.loop.label}"

    override fun visitThrow(expression: IrThrow, data: Nothing?): String =
            "THROW type=${expression.type.render()}"

    override fun visitCallableReference(expression: IrCallableReference, data: Nothing?): String =
            "CALLABLE_REFERENCE '${expression.descriptor}' type=${expression.type.render()} origin=${expression.origin}"

    override fun visitClassReference(expression: IrClassReference, data: Nothing?): String =
            "CLASS_REFERENCE '${expression.descriptor}' type=${expression.type.render()}"

    override fun visitGetClass(expression: IrGetClass, data: Nothing?): String =
            "GET_CLASS type=${expression.type.render()}"

    override fun visitTry(aTry: IrTry, data: Nothing?): String =
            "TRY type=${aTry.type.render()}"

    override fun visitCatch(aCatch: IrCatch, data: Nothing?): String =
            "CATCH parameter=${aCatch.parameter.ref()}"

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Nothing?): String =
            "ERROR_DECL ${declaration.descriptor.javaClass.simpleName} ${declaration.descriptor.ref()}"

    override fun visitErrorExpression(expression: IrErrorExpression, data: Nothing?): String =
            "ERROR_EXPR '${expression.description}' type=${expression.type.render()}"

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: Nothing?): String =
            "ERROR_CALL '${expression.description}' type=${expression.type.render()}"

    companion object {
        val DECLARATION_RENDERER = DescriptorRenderer.withOptions {
            withDefinedIn = false
            overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE
            includePropertyConstant = true
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
            verbose = false
            modifiers = DescriptorRendererModifier.ALL
        }

        val REFERENCE_RENDERER = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES

        internal fun IrDeclaration.name(): String =
                descriptor.let { it.name.toString() }

        internal fun IrDeclaration.renderDeclared(): String =
                DECLARATION_RENDERER.render(this.descriptor)

        internal fun DeclarationDescriptor.ref(): String =
                if (this is ReceiverParameterDescriptor)
                    "<receiver: ${containingDeclaration.ref()}>"
                else
                    REFERENCE_RENDERER.render(this)

        internal fun KotlinType.render(): String =
                DECLARATION_RENDERER.renderType(this)

        internal fun IrDeclaration.renderOrigin(): String =
                if (origin != IrDeclarationOrigin.DEFINED) origin.toString() + " " else ""
    }
}

class DumpIrTreeWithDescriptorsVisitor(out: Appendable): IrElementVisitor<Unit, String> {
    val printer = Printer(out, "  ")
    val elementRenderer = RenderIrElementWithDescriptorsVisitor()

    companion object {
        val ANNOTATIONS_RENDERER = DescriptorRenderer.withOptions {
            verbose = true
            annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
        }
    }

    override fun visitElement(element: IrElement, data: String) {
        element.dumpLabeledSubTree(data)
    }

    override fun visitFile(declaration: IrFile, data: String) {
        declaration.dumpLabeledElementWith(data) {
            if (declaration.fileAnnotations.isNotEmpty()) {
                printer.println("fileAnnotations:")
                indented {
                    declaration.fileAnnotations.forEach {
                        printer.println(ANNOTATIONS_RENDERER.renderAnnotation(it))
                    }
                }
            }
            declaration.declarations.forEach { it.accept(this, "") }
        }
    }

    override fun visitBlock(expression: IrBlock, data: String) {
        if (expression is IrReturnableBlock) {
            printer.println("RETURNABLE BLOCK " + expression.descriptor)
            indented { super.visitBlock(expression, data) }
            return
        }
        super.visitBlock(expression, data)
    }

    override fun visitFunction(declaration: IrFunction, data: String) {
        visitFunctionWithParameters(declaration, data)
    }

    override fun visitConstructor(declaration: IrConstructor, data: String) {
        visitFunctionWithParameters(declaration, data)
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.explicitReceiver?.accept(this, "receiver")
            expression.arguments.forEach { it.accept(this, "") }
        }
    }

    private fun visitFunctionWithParameters(declaration: IrFunction, data: String) {
        declaration.dumpLabeledElementWith(data) {
            declaration.descriptor.valueParameters.forEach { valueParameter ->
                declaration.getDefault(valueParameter)?.accept(this, valueParameter.name.asString())
            }
            declaration.body?.accept(this, "")
        }
    }

    override fun visitEnumEntry(declaration: IrEnumEntry, data: String) {
        declaration.dumpLabeledElementWith(data) {
            declaration.initializerExpression?.accept(this, "init")
            declaration.correspondingClass?.accept(this, "class")
        }
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: String) {
        expression.dumpLabeledElementWith(data) {
            dumpTypeArguments(expression)

            expression.dispatchReceiver?.accept(this, "\$this")
            expression.extensionReceiver?.accept(this, "\$receiver")
            for (valueParameter in expression.descriptor.valueParameters) {
                expression.getValueArgument(valueParameter.index)?.accept(this, valueParameter.name.asString())
            }
        }
    }

    private fun dumpTypeArguments(expression: IrMemberAccessExpression) {
        for (typeParameter in expression.descriptor.original.typeParameters) {
            val typeArgument = expression.getTypeArgument(typeParameter) ?: continue
            val renderedParameter = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.render(typeParameter)
            val renderedType = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.renderType(typeArgument)
            printer.println("$renderedParameter: $renderedType")
        }
    }

    override fun visitGetField(expression: IrGetField, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.receiver?.accept(this, "receiver")
        }
    }

    override fun visitSetField(expression: IrSetField, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.receiver?.accept(this, "receiver")
            expression.value.accept(this, "value")
        }
    }

    override fun visitWhen(expression: IrWhen, data: String) {
        expression.dumpLabeledElementWith(data) {
            expression.branches.forEach {
                it.accept(this, "")
            }
        }
    }

    override fun visitBranch(branch: IrBranch, data: String) {
        branch.dumpLabeledElementWith(data) {
            branch.condition.accept(this, "if")
            branch.result.accept(this, "then")
        }
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: String) {
        loop.dumpLabeledElementWith(data) {
            loop.condition.accept(this, "condition")
            loop.body?.accept(this, "body")
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: String) {
        loop.dumpLabeledElementWith(data) {
            loop.body?.accept(this, "body")
            loop.condition.accept(this, "condition")
        }
    }

    override fun visitTry(aTry: IrTry, data: String) {
        aTry.dumpLabeledElementWith(data) {
            aTry.tryResult.accept(this, "try")
            for (aCatch in aTry.catches) {
                aCatch.accept(this, "")
            }
            aTry.finallyExpression?.accept(this, "finally")
        }
    }

    private inline fun IrElement.dumpLabeledElementWith(label: String, body: () -> Unit) {
        printer.println(accept(elementRenderer, null).withLabel(label))
        indented(body)
    }

    private fun IrElement.dumpLabeledSubTree(label: String) {
        printer.println(accept(elementRenderer, null).withLabel(label))
        indented {
            acceptChildren(this@DumpIrTreeWithDescriptorsVisitor, "")
        }
    }

    private inline fun indented(body: () -> Unit) {
        printer.pushIndent()
        body()
        printer.popIndent()
    }

    private fun String.withLabel(label: String) =
            if (label.isEmpty()) this else "$label: $this"
}