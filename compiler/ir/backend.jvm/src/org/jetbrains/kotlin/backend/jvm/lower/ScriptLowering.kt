/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeCustomPhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal val scriptsToClassesPhase = makeCustomPhase<JvmBackendContext, IrModuleFragment>(
    name = "ScriptsToClasses",
    description = "Put script declarations into classes",
    op = { context, input ->
        ScriptsToClassesLowering(context).lower(input)
    }
)


private class ScriptsToClassesLowering(val context: JvmBackendContext) {

    fun lower(module: IrModuleFragment) {
        val scriptsToClasses = mutableMapOf<IrScript, IrClass>()

        for (irFile in module.files) {
            val iterator = irFile.declarations.listIterator()
            while (iterator.hasNext()) {
                val declaration = iterator.next()
                if (declaration is IrScript) {
                    val scriptClass = prepareScriptClass(irFile, declaration)
                    scriptsToClasses[declaration] = scriptClass
                    iterator.set(scriptClass)
                }
            }
        }

        val symbolRemapper = ScriptsToClassesSymbolRemapper(scriptsToClasses)

        for ((irScript, irScriptClass) in scriptsToClasses) {
            finalizeScriptClass(irScriptClass, irScript, symbolRemapper)
        }
    }

    private fun prepareScriptClass(irFile: IrFile, irScript: IrScript): IrClass {
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
            irScriptClass.superTypes += irScript.baseClass
            irScriptClass.parent = irFile
            irScriptClass.metadata = irScript.metadata
            irScript.targetClass = irScriptClass.symbol
        }
    }

    private fun finalizeScriptClass(irScriptClass: IrClass, irScript: IrScript, symbolRemapper: ScriptsToClassesSymbolRemapper) {
        val typeRemapper = SimpleTypeRemapper(symbolRemapper)
        val scriptTransformer = ScriptToClassTransformer(irScript, irScriptClass, symbolRemapper, typeRemapper, context)
        val lambdaPatcher = ScriptFixLambdasTransformer(irScript, irScriptClass, context)

        fun <E: IrElement> E.patchForClass(): IrElement =
            transform(scriptTransformer, null).transform(lambdaPatcher, ScriptToClassTransformerContext())

        irScriptClass.thisReceiver = irScript.thisReceiver.transform(scriptTransformer, null)

        irScriptClass.addConstructor {
            isPrimary = true
        }.also { irConstructor ->

            fun addConstructorParameter(valueParameter: IrValueParameter, createCorrespondingProperty: Boolean): IrValueParameter {
                val newValueParameter = valueParameter.patchForClass() as IrValueParameter
                irConstructor.valueParameters = irConstructor.valueParameters + newValueParameter
                if (createCorrespondingProperty) {
                    irScriptClass.addSimplePropertyFrom(
                        newValueParameter,
                        IrExpressionBodyImpl(
                            IrGetValueImpl(
                                newValueParameter.startOffset, newValueParameter.endOffset,
                                newValueParameter.type,
                                newValueParameter.symbol,
                                IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER
                            )
                        )
                    )
                }
                return newValueParameter
            }

            irScript.earlierScriptsParameter?.let { earlierScriptdParameter ->
                addConstructorParameter(earlierScriptdParameter, false)
            }
            val copiedExplicitParameters = irScript.explicitCallParameters.map { addConstructorParameter(it, false) }
            irScript.implicitReceiversParameters.forEach { addConstructorParameter(it, false) }
            irScript.providedProperties.forEach { addConstructorParameter(it.first, false) }

            irConstructor.body = context.createIrBuilder(irConstructor.symbol).irBlockBody {
                val baseClassCtor = irScript.baseClass.classOrNull?.owner?.constructors?.firstOrNull()
                // TODO: process situation with multiple constructors (should probably be an error)
                if (baseClassCtor == null) {
                    +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                } else {
                    +irDelegatingConstructorCall(baseClassCtor).also {
                        copiedExplicitParameters.forEachIndexed { idx, valueParameter ->
                            it.putValueArgument(
                                idx,
                                IrGetValueImpl(
                                    valueParameter.startOffset, valueParameter.endOffset,
                                    valueParameter.type,
                                    valueParameter.symbol
                                )
                            )
                        }
                    }
                }
                +IrInstanceInitializerCallImpl(
                    irScript.startOffset, irScript.endOffset,
                    irScriptClass.symbol,
                    context.irBuiltIns.unitType
                )
            }
        }
        var hasMain = false
        irScript.statements.forEach { scriptStatement ->
            when (scriptStatement) {
                is IrVariable -> {
                    val copy = scriptStatement.patchForClass() as IrVariable
                    irScriptClass.addSimplePropertyFrom(copy)
                }
                is IrDeclaration -> {
                    val copy = scriptStatement.patchForClass() as IrDeclaration
                    irScriptClass.declarations.add(copy)
                    // temporary way to avoid name clashes
                    // TODO: remove as soon as main generation become an explicit configuration option
                    if (copy is IrSimpleFunction && copy.name.asString() == "main") {
                        hasMain = true
                    }
                }
                else -> {
                    val transformedStatement = scriptStatement.patchForClass() as IrStatement
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
        if (!hasMain) {
            irScriptClass.addScriptMainFun()
        }

        irScriptClass.annotations += (irScriptClass.parent as IrFile).annotations

        irScript.resultProperty?.owner?.let { irResultProperty ->
            context.state.scriptSpecific.resultFieldName = irResultProperty.name.identifier
            context.state.scriptSpecific.resultTypeString = irResultProperty.backingField?.type?.render()
        }
    }

    private val scriptingJvmPackage by lazy(LazyThreadSafetyMode.PUBLICATION) {
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(context.state.module, FqName("kotlin.script.experimental.jvm"))
    }

    private fun IrClass.addScriptMainFun() {
        val javaLangClass = context.ir.symbols.javaLangClass
        val kClassJava = context.ir.symbols.kClassJava

        val scriptRunnerPackageClass: IrClassSymbol = context.irFactory.buildClass {
            name = Name.identifier("RunnerKt")
            kind = ClassKind.CLASS
            modality = Modality.FINAL
            isInline = false
        }.apply {
            parent = scriptingJvmPackage
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addFunction("runCompiledScript", context.irBuiltIns.unitType, isStatic = true).apply {
                addValueParameter("scriptClass", javaLangClass.starProjectedType)
                addValueParameter {
                    name = Name.identifier("args")
                    type = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.stringType)
                    origin = IrDeclarationOrigin.DEFINED
                    varargElementType = context.irBuiltIns.anyNType
                }
            }
        }.symbol

        val scriptRunHelper: IrSimpleFunctionSymbol =
            scriptRunnerPackageClass.functions.single { it.owner.name.asString() == "runCompiledScript" }

        val scriptClassRef = IrClassReferenceImpl(
            startOffset, endOffset, context.irBuiltIns.kClassClass.starProjectedType, context.irBuiltIns.kClassClass, this.defaultType
        )

        addFunction {
            name = Name.identifier("main")
            visibility = DescriptorVisibilities.PUBLIC
            returnType = context.irBuiltIns.unitType
            modality = Modality.FINAL
        }.also { mainFun ->
            val args = mainFun.addValueParameter {
                name = Name.identifier("args")
                type = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.stringType)
            }
            mainFun.body = context.createIrBuilder(mainFun.symbol).run {
                irExprBody(
                    irCall(scriptRunHelper).apply {
                        putValueArgument(
                            0,
                            irGet(javaLangClass.starProjectedType, null, kClassJava.owner.getter!!.symbol).apply {
                                extensionReceiver = scriptClassRef
                            }
                        )
                        putValueArgument(
                            1,
                            IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, args.type, args.symbol)
                        )
                    }
                )
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

                property.addDefaultGetter(this, context.irBuiltIns)
            }
        }
    }
}

data class ScriptToClassTransformerContext(
    val insideTopLevelDestructuringDeclaration: Boolean = false,
    val valueParameterToReplaceWithScript: IrValueParameter? = null
)

private class ScriptToClassTransformer(
    val irScript: IrScript,
    val irScriptClass: IrClass,
    val symbolRemapper: SymbolRemapper,
    val typeRemapper: TypeRemapper,
    val context: JvmBackendContext
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

    private fun getAccessCallForEarlierScript(expression: IrDeclarationReference, maybeScriptType: IrType): IrExpression? {
        if (irScript.earlierScripts?.isEmpty() != false) return null
        val scriptSymbol = maybeScriptType.classifierOrNull ?: return null
        val scriptSymbolOwner = scriptSymbol.owner
        val earlierScriptIndex = when {
            scriptSymbolOwner is IrScript ->
                irScript.earlierScripts!!.indexOfFirst { it == scriptSymbol }
            (scriptSymbolOwner as? IrClass)?.origin == IrDeclarationOrigin.SCRIPT_CLASS -> {
                irScript.earlierScripts!!.indexOfFirst { it.owner.targetClass == scriptSymbol }
            }
            else -> return null
        }
        if (earlierScriptIndex >= 0) {
            val objArray = context.irBuiltIns.arrayClass
            val objArrayGet = objArray.functions.single { it.owner.name == OperatorNameConventions.GET }
            val builder = context.createIrBuilder(expression.symbol)
            val getPrevScriptObjectExpression = builder.irCall(objArrayGet).apply {
                dispatchReceiver = builder.irGet(objArray.defaultType, irScript.earlierScriptsParameter!!.symbol)
                putValueArgument(0, earlierScriptIndex.toIrConst(objArrayGet.owner.valueParameters.first().type))
            }
            val prevScriptClassType =
                when {
                    scriptSymbolOwner is IrScript -> scriptSymbolOwner.targetClass?.owner
                    (scriptSymbolOwner as? IrClass)?.origin == IrDeclarationOrigin.SCRIPT_CLASS -> scriptSymbolOwner
                    else -> null
                }
            return if (prevScriptClassType == null) getPrevScriptObjectExpression
            else builder.irImplicitCast(getPrevScriptObjectExpression, prevScriptClassType.defaultType)
        }
        return null
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        if (irScript.earlierScripts != null) {
            val receiver = expression.receiver
            if (receiver is IrGetValue && receiver.symbol.owner.name == SpecialNames.THIS) {
                val newReceiver = getAccessCallForEarlierScript(expression, receiver.type)
                if (newReceiver != null) {
                    val newGetField =
                        IrGetFieldImpl(expression.startOffset, expression.endOffset, expression.symbol, expression.type, newReceiver)
                    return super.visitGetField(newGetField)
                }
            }
        }
        return super.visitGetField(expression)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (irScript.earlierScripts != null) {
            val target = expression.symbol.owner
            val receiver: IrValueParameter? = target.dispatchReceiverParameter
            if (receiver?.name == SpecialNames.THIS) {
                val newReceiver = getAccessCallForEarlierScript(expression, receiver.type)
                if (newReceiver != null) {
                    expression.dispatchReceiver = newReceiver
                }
            }
        }
        return super.visitCall(expression)
    }
}

private class ScriptFixLambdasTransformer(
    val irScript: IrScript,
    val irScriptClass: IrClass,
    val context: JvmBackendContext
) : IrElementTransformer<ScriptToClassTransformerContext> {

    private fun unexpectedElement(element: IrElement): Nothing =
        throw IllegalArgumentException("Unsupported element type: $element")

    override fun visitElement(element: IrElement, data: ScriptToClassTransformerContext): IrElement = unexpectedElement(element)

    override fun visitModuleFragment(declaration: IrModuleFragment, data: ScriptToClassTransformerContext): IrModuleFragment =
        unexpectedElement(declaration)

    override fun visitExternalPackageFragment(
        declaration: IrExternalPackageFragment,
        data: ScriptToClassTransformerContext
    ): IrExternalPackageFragment =
        unexpectedElement(declaration)

    override fun visitFile(declaration: IrFile, data: ScriptToClassTransformerContext): IrFile = unexpectedElement(declaration)
    override fun visitScript(declaration: IrScript, data: ScriptToClassTransformerContext): IrScript = unexpectedElement(declaration)

    override fun visitGetValue(expression: IrGetValue, data: ScriptToClassTransformerContext): IrExpression {
        if (data.valueParameterToReplaceWithScript == expression.symbol.owner) {
            val newGetValue = IrGetValueImpl(
                expression.startOffset, expression.endOffset,
                expression.type,
                irScriptClass.thisReceiver!!.symbol,
                expression.origin
            )
            return super.visitGetValue(newGetValue, data)
        } else return super.visitGetValue(expression, data)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: ScriptToClassTransformerContext): IrSimpleFunction =
        with(declaration) {
            if (data.insideTopLevelDestructuringDeclaration && origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) {
                visibility = DescriptorVisibilities.LOCAL
                val dataForChildren =
                    if (dispatchReceiverParameter?.type == irScriptClass.defaultType) {
                        val oldDispatchReceiver = dispatchReceiverParameter
                        dispatchReceiverParameter = null
                        data.copy(valueParameterToReplaceWithScript = oldDispatchReceiver)
                    } else data
                super.visitSimpleFunction(this, dataForChildren)
            } else {
                super.visitSimpleFunction(this, data)
            }
        } as IrSimpleFunction

    override fun visitComposite(expression: IrComposite, data: ScriptToClassTransformerContext): IrComposite {
        val dataForChildren =
            if (expression.origin == IrStatementOrigin.DESTRUCTURING_DECLARATION &&
                expression.statements.firstIsInstanceOrNull<IrDeclaration>()?.parent == irScriptClass
            ) {
                data.copy(insideTopLevelDestructuringDeclaration = true)
            } else {
                data
            }
        return super.visitComposite(expression, dataForChildren) as IrComposite
    }
}


private class ScriptsToClassesSymbolRemapper(
    val scriptsToClasses: Map<IrScript, IrClass>
) : SymbolRemapper.Empty() {
    override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol =
        (symbol.owner as? IrScript)?.let { scriptsToClasses[it] }?.symbol ?: symbol
}

private inline fun IrClass.addAnonymousInitializer(builder: IrFunctionBuilder.() -> Unit = {}): IrAnonymousInitializer =
    IrFunctionBuilder().run {
        builder()
        returnType = defaultType
        IrAnonymousInitializerImpl(
            startOffset, endOffset, origin,
            IrAnonymousInitializerSymbolImpl()
        )
    }.also { anonymousInitializer ->
        declarations.add(anonymousInitializer)
        anonymousInitializer.parent = this@addAnonymousInitializer
    }

