/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.ClosureAnnotator
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeCustomPhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmInnerClassesSupport
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmBackendErrors
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal val scriptsToClassesPhase = makeCustomPhase<JvmBackendContext, IrModuleFragment>(
    name = "ScriptsToClasses",
    description = "Put script declarations into classes",
    op = { context, input ->
        ScriptsToClassesLowering(context, context.innerClassesSupport).lower(input)
    }
)


private class ScriptsToClassesLowering(val context: JvmBackendContext, val innerClassesSupport: JvmInnerClassesSupport) {

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

    private fun collectCapturingClasses(irScript: IrScript): Set<IrClassImpl> {
        val annotator = ClosureAnnotator(irScript, irScript)
        val capturingClasses = mutableSetOf<IrClassImpl>()
        val collector = object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                if (declaration is IrClassImpl && !declaration.isInner) {
                    val closure = annotator.getClassClosure(declaration)
                    if (closure.capturedValues.singleOrNull()?.owner?.type == irScript.thisReceiver.type) {
                        fun reportError(factory: KtDiagnosticFactory1<String>, name: Name? = null) {
                            context.ktDiagnosticReporter.at(declaration).report(factory, (name ?: declaration.name).asString())
                        }
                        when {
                            declaration.isInterface -> reportError(JvmBackendErrors.SCRIPT_CAPTURING_INTERFACE)
                            declaration.isEnumClass -> reportError(JvmBackendErrors.SCRIPT_CAPTURING_ENUM)
                            declaration.isEnumEntry -> reportError(JvmBackendErrors.SCRIPT_CAPTURING_ENUM_ENTRY)
                            // TODO: ClosureAnnotator is not catching companion's closures, so the following reporting never happens. Make it work or drop
                            declaration.isCompanion -> reportError(JvmBackendErrors.SCRIPT_CAPTURING_OBJECT, SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
                            declaration.kind.isSingleton -> reportError(JvmBackendErrors.SCRIPT_CAPTURING_OBJECT)

                            declaration.isClass ->
                                if (declaration.parent != irScript) {
                                    context.ktDiagnosticReporter.at(declaration).report(
                                        JvmBackendErrors.SCRIPT_CAPTURING_NESTED_CLASS,
                                        declaration.name.asString(),
                                        ((declaration.parent as? IrDeclarationWithName)?.name ?: SpecialNames.NO_NAME_PROVIDED).asString()
                                    )
                                } else {
                                    capturingClasses.add(declaration)
                                }
                        }
                    }
                }
                super.visitClass(declaration)
            }
        }
        for (statement in irScript.statements) {
            if (statement is IrClassImpl) {
                collector.visitClass(statement)
            }
        }
        return capturingClasses
    }

    private fun finalizeScriptClass(irScriptClass: IrClass, irScript: IrScript, symbolRemapper: ScriptsToClassesSymbolRemapper) {
        val typeRemapper = SimpleTypeRemapper(symbolRemapper)
        val capturingClasses = collectCapturingClasses(irScript)
        val scriptTransformer = ScriptToClassTransformer(
            irScript,
            irScriptClass,
            typeRemapper,
            context,
            capturingClasses,
            innerClassesSupport
        )
        val lambdaPatcher = ScriptFixLambdasTransformer(irScript, irScriptClass, context)

        irScriptClass.thisReceiver = scriptTransformer.scriptClassReceiver

        val defaultContext = ScriptToClassTransformerContext(irScriptClass.thisReceiver?.symbol, null, null)
        fun <E: IrElement> E.patchForClass(): IrElement {
            return transform(scriptTransformer, defaultContext)
                .transform(lambdaPatcher, ScriptFixLambdasTransformerContext())
        }

        irScript.constructor?.patchForClass()?.safeAs<IrConstructor>()!!.also { constructor ->
            val explicitParamsStartIndex = if (irScript.earlierScriptsParameter == null) 0 else 1
            val explicitParameters = constructor.valueParameters.subList(
                explicitParamsStartIndex,
                irScript.explicitCallParameters.size + explicitParamsStartIndex
            )
            constructor.body = context.createIrBuilder(constructor.symbol).irBlockBody {
                val baseClassCtor = irScript.baseClass.classOrNull?.owner?.constructors?.firstOrNull()
                // TODO: process situation with multiple constructors (should probably be an error)
                if (baseClassCtor == null) {
                    +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                } else {
                    +irDelegatingConstructorCall(baseClassCtor).also {
                        explicitParameters.forEachIndexed { idx, valueParameter ->
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
            irScriptClass.declarations.add(constructor)
            constructor.parent = irScriptClass
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
        initializer: IrExpressionBody? = null
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
    val valueParameterForScriptThis: IrValueParameterSymbol?,
    val fieldForScriptThis: IrFieldSymbol?,
    val valueParameterForFieldReceiver: IrValueParameterSymbol?
)

data class ScriptFixLambdasTransformerContext(
    val insideTopLevelDestructuringDeclaration: Boolean = false,
    val valueParameterToReplaceWithScript: IrValueParameter? = null
)

private class ScriptToClassTransformer(
    val irScript: IrScript,
    val irScriptClass: IrClass,
    val typeRemapper: TypeRemapper,
    val context: JvmBackendContext,
    val capturingClasses: Set<IrClassImpl>,
    val innerClassesSupport: JvmInnerClassesSupport
) : IrElementTransformer<ScriptToClassTransformerContext> {

    private fun IrType.remapType() = typeRemapper.remapType(this)

    val capturingClassesConstructors = mutableMapOf<IrConstructor, IrClassImpl>().apply {
        capturingClasses.forEach { c ->
            c.declarations.forEach { d ->
                if (d is IrConstructor) {
                    put(d, c)
                }
            }
        }
    }

    val scriptClassReceiver =
        irScript.thisReceiver.transform(this, ScriptToClassTransformerContext(null, null, null))

    private fun IrDeclaration.transformParent() {
        if (parent == irScript) {
            parent = irScriptClass
        }
    }

    private fun IrMutableAnnotationContainer.transformAnnotations(data: ScriptToClassTransformerContext) {
        annotations = annotations.transform(data)
    }

    private inline fun <reified T : IrElement> T.transform(data: ScriptToClassTransformerContext) =
        transform(this@ScriptToClassTransformer, data) as T

    private inline fun <reified T : IrElement> List<T>.transform(data: ScriptToClassTransformerContext) =
        map { it.transform(data) }

    private fun <T : IrFunction> T.transformFunctionChildren(data: ScriptToClassTransformerContext): T =
        apply {
            transformAnnotations(data)
            typeRemapper.withinScope(this) {
                dispatchReceiverParameter = dispatchReceiverParameter?.transform(data)
                val dataForChildren =
                    if (dispatchReceiverParameter?.type != scriptClassReceiver.type) data
                    else ScriptToClassTransformerContext(dispatchReceiverParameter!!.symbol, null, null)
                extensionReceiverParameter = extensionReceiverParameter?.transform(dataForChildren)
                returnType = returnType.remapType()
                valueParameters = valueParameters.transform(dataForChildren)
                body = body?.transform(dataForChildren)
            }
        }

    private fun IrTypeParameter.remapSuperTypes(): IrTypeParameter = apply {
        superTypes = superTypes.map { it.remapType() }
    }

    private fun unexpectedElement(element: IrElement): Nothing =
        throw IllegalArgumentException("Unsupported element type: $element")

    override fun visitElement(element: IrElement, data: ScriptToClassTransformerContext): IrElement = unexpectedElement(element)

    override fun visitModuleFragment(declaration: IrModuleFragment, data: ScriptToClassTransformerContext): IrModuleFragment = unexpectedElement(declaration)
    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: ScriptToClassTransformerContext) = unexpectedElement(declaration)
    override fun visitFile(declaration: IrFile, data: ScriptToClassTransformerContext): IrFile = unexpectedElement(declaration)
    override fun visitScript(declaration: IrScript, data: ScriptToClassTransformerContext): IrStatement = unexpectedElement(declaration)

    override fun visitDeclaration(declaration: IrDeclarationBase, data: ScriptToClassTransformerContext): IrStatement = declaration.apply {
        transformParent()
        transformAnnotations(data)
        transformChildren(this@ScriptToClassTransformer, data)
    }

    override fun visitClass(declaration: IrClass, data: ScriptToClassTransformerContext): IrClass = declaration.apply {
        superTypes = superTypes.map {
            it.remapType()
        }
        transformParent()
        var dataForChildren = data
        (declaration as? IrClassImpl)?.let {
            if (it in capturingClasses) {
                it.isInner = true
                dataForChildren =
                    ScriptToClassTransformerContext(
                        null, innerClassesSupport.getOuterThisField(it).symbol, it.thisReceiver?.symbol
                    )
            }
        }
        transformAnnotations(dataForChildren)
        transformChildren(this@ScriptToClassTransformer, dataForChildren)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: ScriptToClassTransformerContext): IrSimpleFunction =
        declaration.apply {
            transformParent()
            val dataForChildren =
                if (data.valueParameterForFieldReceiver?.owner?.type.let { it != null && it == this.dispatchReceiverParameter?.type })
                    ScriptToClassTransformerContext(
                        data.valueParameterForScriptThis, data.fieldForScriptThis, this.dispatchReceiverParameter?.symbol
                    )
                else data
            transformFunctionChildren(dataForChildren)
        }

    override fun visitConstructor(declaration: IrConstructor, data: ScriptToClassTransformerContext): IrConstructor = declaration.apply {
        if (declaration in capturingClassesConstructors) {
            declaration.dispatchReceiverParameter =
                IrValueParameterBuilder().run<IrValueParameterBuilder, IrValueParameter> {
                    name = SpecialNames.THIS
                    type = scriptClassReceiver.type
                    declaration.factory.createValueParameter(
                        startOffset, endOffset, IrDeclarationOrigin.INSTANCE_RECEIVER,
                        IrValueParameterSymbolImpl(),
                        name, index, type, varargElementType, isCrossInline, isNoinline, isHidden, isAssignable
                    ).also {
                        it.parent = declaration
                    }
                }
        }
        transformParent()
        transformFunctionChildren(data)
    }

    override fun visitVariable(declaration: IrVariable, data: ScriptToClassTransformerContext): IrVariable = declaration.apply {
        type = type.remapType()
        visitDeclaration(declaration, data)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter, data: ScriptToClassTransformerContext): IrTypeParameter =
        declaration.apply {
            remapSuperTypes()
            visitDeclaration(declaration, data)
        }

    override fun visitValueParameter(declaration: IrValueParameter, data: ScriptToClassTransformerContext): IrValueParameter =
        declaration.apply {
            type = type.remapType()
            varargElementType = varargElementType?.remapType()
            visitDeclaration(declaration, data)
        }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: ScriptToClassTransformerContext): IrTypeAlias = declaration.apply {
        expandedType = expandedType.remapType()
        visitDeclaration(declaration, data)
    }

    override fun visitVararg(expression: IrVararg, data: ScriptToClassTransformerContext): IrVararg = expression.apply {
        type = type.remapType()
        varargElementType = varargElementType.remapType()
        transformChildren(this@ScriptToClassTransformer, data)
    }

    override fun visitSpreadElement(spread: IrSpreadElement, data: ScriptToClassTransformerContext): IrSpreadElement = spread.apply {
        transformChildren(this@ScriptToClassTransformer, data)
    }

    override fun visitExpression(expression: IrExpression, data: ScriptToClassTransformerContext): IrExpression = expression.apply {
        type = type.remapType()
        transformChildren(this@ScriptToClassTransformer, data)
    }

    override fun visitClassReference(expression: IrClassReference, data: ScriptToClassTransformerContext): IrClassReference =
        expression.apply {
            type = type.remapType()
            classType = classType.remapType()
            transformChildren(this@ScriptToClassTransformer, data)
        }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: ScriptToClassTransformerContext): IrTypeOperatorCall =
        expression.apply {
            type = type.remapType()
            typeOperand = typeOperand.remapType()
            transformChildren(this@ScriptToClassTransformer, data)
        }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: ScriptToClassTransformerContext): IrExpression =
        expression.apply {
            for (i in 0 until typeArgumentsCount) {
                putTypeArgument(i, getTypeArgument(i)?.remapType())
            }
            visitExpression(expression, data)
        }

    override fun visitConstructorCall(expression: IrConstructorCall, data: ScriptToClassTransformerContext): IrExpression {
        capturingClassesConstructors.keys.find {  it.symbol == expression.symbol }?.let {
            expression.dispatchReceiver =
                getAccessCallForScriptInstance(data, expression.startOffset, expression.endOffset, expression.origin)
        }
        return super.visitConstructorCall(expression, data) as IrExpression
    }

    private fun getAccessCallForScriptInstance(
        data: ScriptToClassTransformerContext, startOffset: Int, endOffset: Int, origin: IrStatementOrigin?
    ) = when {
        data.fieldForScriptThis != null ->
            IrGetFieldImpl(
                startOffset, endOffset,
                data.fieldForScriptThis,
                scriptClassReceiver.type,
                origin
            ).apply {
                receiver =
                    IrGetValueImpl(
                        startOffset, endOffset,
                        data.valueParameterForFieldReceiver!!.owner.type,
                        data.valueParameterForFieldReceiver,
                        origin
                    )
            }
        data.valueParameterForScriptThis != null ->
            IrGetValueImpl(
                startOffset, endOffset,
                scriptClassReceiver.type,
                data.valueParameterForScriptThis,
                origin
            )
        else -> error("Unexpected script transformation state: $data")
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

    override fun visitGetField(expression: IrGetField, data: ScriptToClassTransformerContext): IrExpression {
        if (irScript.earlierScripts != null) {
            val receiver = expression.receiver
            if (receiver is IrGetValue && receiver.symbol.owner.name == SpecialNames.THIS) {
                val newReceiver = getAccessCallForEarlierScript(expression, receiver.type)
                if (newReceiver != null) {
                    val newGetField =
                        IrGetFieldImpl(expression.startOffset, expression.endOffset, expression.symbol, expression.type, newReceiver)
                    return super.visitGetField(newGetField, data)
                }
            }
        }
        return super.visitGetField(expression, data)
    }

    override fun visitCall(expression: IrCall, data: ScriptToClassTransformerContext): IrExpression {
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
        return super.visitCall(expression, data) as IrExpression
    }


}

private class ScriptFixLambdasTransformer(
    val irScript: IrScript,
    val irScriptClass: IrClass,
    val context: JvmBackendContext
) : IrElementTransformer<ScriptFixLambdasTransformerContext> {

    private fun unexpectedElement(element: IrElement): Nothing =
        throw IllegalArgumentException("Unsupported element type: $element")

    override fun visitElement(element: IrElement, data: ScriptFixLambdasTransformerContext): IrElement = unexpectedElement(element)

    override fun visitModuleFragment(declaration: IrModuleFragment, data: ScriptFixLambdasTransformerContext): IrModuleFragment =
        unexpectedElement(declaration)

    override fun visitExternalPackageFragment(
        declaration: IrExternalPackageFragment,
        data: ScriptFixLambdasTransformerContext
    ): IrExternalPackageFragment =
        unexpectedElement(declaration)

    override fun visitFile(declaration: IrFile, data: ScriptFixLambdasTransformerContext): IrFile = unexpectedElement(declaration)
    override fun visitScript(declaration: IrScript, data: ScriptFixLambdasTransformerContext): IrScript = unexpectedElement(declaration)

    override fun visitGetValue(expression: IrGetValue, data: ScriptFixLambdasTransformerContext): IrExpression {
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

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: ScriptFixLambdasTransformerContext): IrSimpleFunction =
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

    override fun visitComposite(expression: IrComposite, data: ScriptFixLambdasTransformerContext): IrComposite {
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

