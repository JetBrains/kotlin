/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.WrappedClassDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal val scriptToClassPhase = makeIrModulePhase(
    ::ScriptToClassLowering,
    name = "ScriptToClass",
    description = "Put script declarations into a class",
    stickyPostconditions = setOf(::checkAllFileLevelDeclarationsAreClasses)
)

private class ScriptToClassLowering(val context: JvmBackendContext) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.declarations.replaceAll { declaration ->
            if (declaration is IrScript) makeScriptClass(irFile, declaration)
            else declaration
        }
    }

    private fun makeScriptClass(irFile: IrFile, irScript: IrScript): IrClass {
        val fileEntry = irFile.fileEntry
        return context.irFactory.buildClass {
            startOffset = 0
            endOffset = fileEntry.maxOffset
            origin = IrDeclarationOrigin.SCRIPT_CLASS
            name = irScript.name
            kind = ClassKind.CLASS
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
        }.also { irScriptClass ->
            irScriptClass.superTypes += context.irBuiltIns.anyType
            irScriptClass.parent = irFile
            irScriptClass.createImplicitParameterDeclarationWithWrappedDescriptor()
            val symbolRemapper = ScriptToClassSymbolRemapper(irScript.symbol, irScriptClass.symbol)
            val typeRemapper = ScriptTypeRemapper(symbolRemapper)
            val scriptTransformer = ScriptToClassTransformer(irScript, irScriptClass, symbolRemapper, typeRemapper)
            irScriptClass.thisReceiver = irScript.thisReceiver.run {
                transform(scriptTransformer, null)
            }
            irScriptClass.addConstructor {
                isPrimary = true
            }.also { irConstructor ->
                irScript.explicitCallParameters.forEach { scriptCallParameter ->
                    val callParameter = irConstructor.addValueParameter {
                        updateFrom(scriptCallParameter)
                        name = scriptCallParameter.name
                    }
                    irScriptClass.addSimplePropertyFrom(
                        callParameter,
                        IrExpressionBodyImpl(
                            IrGetValueImpl(
                                callParameter.startOffset, callParameter.endOffset,
                                callParameter.type,
                                callParameter.symbol,
                                IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
                            )
                        )
                    )
                }

                irConstructor.body = context.createIrBuilder(irConstructor.symbol).irBlockBody {
                    +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                    +IrInstanceInitializerCallImpl(
                        irScript.startOffset, irScript.endOffset,
                        irScriptClass.symbol,
                        context.irBuiltIns.unitType
                    )
                }
            }
            irScript.statements.forEach { scriptStatement ->
                when (scriptStatement) {
                    is IrVariable -> irScriptClass.addSimplePropertyFrom(scriptStatement)
                    is IrDeclaration -> {
                        val copy = scriptStatement.transform(scriptTransformer, null) as IrDeclaration
                        irScriptClass.declarations.add(copy)
                    }
                    else -> {
                        val transformedStatement = scriptStatement.transformStatement(scriptTransformer)
                        irScriptClass.addAnonymousInitializer().also { irInitializer ->
                            irInitializer.body =
                                context.createIrBuilder(irInitializer.symbol).irBlockBody {
                                    if (transformedStatement is IrComposite) {
                                        for (statement in transformedStatement.statements)
                                            +statement
                                    } else {
                                        +transformedStatement
                                    }
                                }
                        }
                    }
                }
            }
            irScriptClass.annotations += irFile.annotations
            irScriptClass.metadata = irFile.metadata

            irScript.resultProperty?.owner?.let { irResultProperty ->
                context.state.scriptSpecific.resultFieldName = irResultProperty.name.identifier
                context.state.scriptSpecific.resultTypeString = irResultProperty.backingField?.type?.render()
            }
        }
    }

    private fun IrClass.addSimplePropertyFrom(
        from: IrValueDeclaration,
        initializer: IrExpressionBodyImpl? = null
    ) {
        addProperty {
            updateFrom(from)
            name = from.name
        }.also { property ->
            property.backingField = context.irFactory.buildField {
                name = from.name
                type = from.type
                visibility = DescriptorVisibilities.PROTECTED
            }.also { field ->
                field.parent = this
                if (initializer != null) {
                    field.initializer = initializer
                }

                property.addSimpleFieldGetter(from.type, this, field)
            }
        }
    }

    private fun IrProperty.addSimpleFieldGetter(type: IrType, irScriptClass: IrClass, field: IrField) =
        addGetter {
            returnType = type
        }.apply {
            dispatchReceiverParameter = irScriptClass.thisReceiver!!.copyTo(this)
            body = IrBlockBodyImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(
                    IrReturnImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        context.irBuiltIns.nothingType,
                        symbol,
                        IrGetFieldImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            field.symbol,
                            type,
                            IrGetValueImpl(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                dispatchReceiverParameter!!.type,
                                dispatchReceiverParameter!!.symbol
                            )
                        )
                    )
                )
            )
        }
}

private class ScriptToClassTransformer(
    val irScript: IrScript,
    val irScriptClass: IrClass,
    val symbolRemapper: SymbolRemapper = ScriptToClassSymbolRemapper(irScript.symbol, irScriptClass.symbol),
    val typeRemapper: TypeRemapper = ScriptTypeRemapper(symbolRemapper)
) : IrElementTransformerVoid() {

    private fun IrType.remapType() = typeRemapper.remapType(this)

    private fun IrDeclaration.transformParent() {
        if (parent == irScript) {
            parent = irScriptClass
        }
    }

    private fun IrMutableAnnotationContainer.transformAnnotations() {
        annotations = annotations.transform()
    }

    private inline fun <reified T : IrElement> T.transform() =
        transform(this@ScriptToClassTransformer, null) as T

    private inline fun <reified T : IrElement> List<T>.transform() =
        map { it.transform() }

    private fun <T : IrFunction> T.transformFunctionChildren(): T =
        apply {
            transformAnnotations()
            typeRemapper.withinScope(this) {
                dispatchReceiverParameter = dispatchReceiverParameter?.transform()
                extensionReceiverParameter = extensionReceiverParameter?.transform()
                returnType = returnType.remapType()
                valueParameters = valueParameters.transform()
                body = body?.transform()
            }
        }

    private fun IrTypeParameter.remapSuperTypes(): IrTypeParameter = apply {
        superTypes = superTypes.map { it.remapType() }
    }

    private fun unexpectedElement(element: IrElement): Nothing =
        throw IllegalArgumentException("Unsupported element type: $element")

    override fun visitElement(element: IrElement): IrElement = unexpectedElement(element)

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment = unexpectedElement(declaration)
    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment) = unexpectedElement(declaration)
    override fun visitFile(declaration: IrFile): IrFile = unexpectedElement(declaration)
    override fun visitScript(declaration: IrScript): IrStatement = unexpectedElement(declaration)

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement = declaration.apply {
        transformParent()
        transformAnnotations()
        transformChildren(this@ScriptToClassTransformer, null)
    }

    override fun visitClass(declaration: IrClass): IrClass = declaration.apply {
        superTypes = superTypes.map {
            it.remapType()
        }
        visitDeclaration(declaration)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction = declaration.apply {
        transformParent()
        transformFunctionChildren()
//        transformChildren(this@ScriptToClassTransformer, null)
    }

    override fun visitConstructor(declaration: IrConstructor): IrConstructor = declaration.apply {
        transformParent()
        transformFunctionChildren()
    }

    override fun visitVariable(declaration: IrVariable): IrVariable = declaration.apply {
        type = type.remapType()
        visitDeclaration(declaration)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter): IrTypeParameter = declaration.apply {
        remapSuperTypes()
        visitDeclaration(declaration)
    }

    override fun visitValueParameter(declaration: IrValueParameter): IrValueParameter = declaration.apply {
        type = type.remapType()
        varargElementType = varargElementType?.remapType()
        visitDeclaration(declaration)
    }

    override fun visitTypeAlias(declaration: IrTypeAlias): IrTypeAlias = declaration.apply {
        expandedType = expandedType.remapType()
        visitDeclaration(declaration)
    }

    override fun visitVararg(expression: IrVararg): IrVararg = expression.apply {
        type = type.remapType()
        varargElementType = varargElementType.remapType()
        transformChildren(this@ScriptToClassTransformer, null)
    }

    override fun visitSpreadElement(spread: IrSpreadElement): IrSpreadElement = spread.apply {
        transformChildren(this@ScriptToClassTransformer, null)
    }

    override fun visitExpression(expression: IrExpression): IrExpression = expression.apply {
        type = type.remapType()
        transformChildren(this@ScriptToClassTransformer, null)
    }

    override fun visitClassReference(expression: IrClassReference): IrClassReference = expression.apply {
        type = type.remapType()
        classType = classType.remapType()
        transformChildren(this@ScriptToClassTransformer, null)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrTypeOperatorCall = expression.apply {
        type = type.remapType()
        typeOperand = typeOperand.remapType()
        transformChildren(this@ScriptToClassTransformer, null)
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>): IrExpression = expression.apply {
        for (i in 0 until typeArgumentsCount) {
            putTypeArgument(i, getTypeArgument(i)?.remapType())
        }
        visitExpression(expression)
    }
}

private class ScriptToClassSymbolRemapper(
    val irScriptSymbol: IrScriptSymbol,
    val irScriptClassSymbol: IrClassSymbol
) : SymbolRemapper.Empty() {
    override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol =
        if (symbol != irScriptSymbol) symbol
        else irScriptClassSymbol
}

class ScriptTypeRemapper(
    private val symbolRemapper: SymbolRemapper
) : TypeRemapper {

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
        // TODO
    }

    override fun leaveScope() {
        // TODO
    }

    override fun remapType(type: IrType): IrType =
        if (type !is IrSimpleType)
            type
        else {
            val symbol = symbolRemapper.getReferencedClassifier(type.classifier)
            val arguments = type.arguments.map { remapTypeArgument(it) }
            if (symbol == type.classifier && arguments == type.arguments)
                type
            else {
                IrSimpleTypeImpl(
                    null,
                    symbol,
                    type.hasQuestionMark,
                    arguments,
                    type.annotations,
                    type.abbreviation?.remapTypeAbbreviation()
                )
            }
        }

    private fun remapTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument =
        if (typeArgument is IrTypeProjection)
            makeTypeProjection(this.remapType(typeArgument.type), typeArgument.variance)
        else
            typeArgument

    private fun IrTypeAbbreviation.remapTypeAbbreviation() =
        IrTypeAbbreviationImpl(
            symbolRemapper.getReferencedTypeAlias(typeAlias),
            hasQuestionMark,
            arguments.map { remapTypeArgument(it) },
            annotations
        )
}

private inline fun IrClass.addAnonymousInitializer(builder: IrFunctionBuilder.() -> Unit = {}): IrAnonymousInitializer =
    IrFunctionBuilder().run {
        builder()
        returnType = defaultType
        IrAnonymousInitializerImpl(
            startOffset, endOffset, origin,
            IrAnonymousInitializerSymbolImpl(WrappedClassDescriptor())
        )
    }.also { anonymousInitializer ->
        declarations.add(anonymousInitializer)
        anonymousInitializer.parent = this@addAnonymousInitializer
    }

