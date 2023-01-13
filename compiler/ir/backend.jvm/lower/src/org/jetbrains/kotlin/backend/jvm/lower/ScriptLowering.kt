/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.ClosureAnnotator
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeCustomPhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmInnerClassesSupport
import org.jetbrains.kotlin.backend.jvm.ir.propertyIfAccessor
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
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedKotlinType
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmBackendErrors
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

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
            // TODO fix parents in script classes
            irScriptClass.patchDeclarationParents(irScript.parent)
        }
    }

    private fun prepareScriptClass(irFile: IrFile, irScript: IrScript): IrClass {
        val fileEntry = irFile.fileEntry
        return context.irFactory.buildClass {
            startOffset = 0
            endOffset = fileEntry.maxOffset
            origin = IrDeclarationOrigin.SCRIPT_CLASS
            name = irScript.name.let {
                if (it.isSpecial) {
                    NameUtils.getScriptNameForFile(irScript.name.asStringStripSpecialMarkers().removePrefix("script-"))
                } else it
            }
            kind = ClassKind.CLASS
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
        }.also { irScriptClass ->
            irScriptClass.superTypes += (irScript.baseClass ?: context.irBuiltIns.anyNType)
            irScriptClass.parent = irFile
            irScriptClass.metadata = irScript.metadata
            irScript.targetClass = irScriptClass.symbol
        }
    }

    private fun collectCapturingClasses(irScript: IrScript, typeRemapper: SimpleTypeRemapper): Set<IrClassImpl> {
        val annotator = ClosureAnnotator(irScript, irScript)
        val capturingClasses = mutableSetOf<IrClassImpl>()
        val collector = object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                if (declaration is IrClassImpl && !declaration.isInner) {
                    val closure = annotator.getClassClosure(declaration)
                    val scriptsReceivers = mutableSetOf<IrType>().also {
                        it.addIfNotNull(irScript.thisReceiver?.type)
                    }
                    irScript.earlierScripts?.forEach { scriptsReceivers.addIfNotNull(it.owner.thisReceiver?.type) }
                    irScript.implicitReceiversParameters.forEach {
                        scriptsReceivers.add(it.type)
                        scriptsReceivers.add(typeRemapper.remapType(it.type))
                    }

                    if (closure.capturedValues.any { it.owner.type in scriptsReceivers }) {
                        fun reportError(factory: KtDiagnosticFactory1<String>, name: Name? = null) {
                            context.ktDiagnosticReporter.at(declaration).report(factory, (name ?: declaration.name).asString())
                        }
                        when {
                            declaration.isInterface -> reportError(JvmBackendErrors.SCRIPT_CAPTURING_INTERFACE)
                            declaration.isEnumClass -> reportError(JvmBackendErrors.SCRIPT_CAPTURING_ENUM)
                            declaration.isEnumEntry -> reportError(JvmBackendErrors.SCRIPT_CAPTURING_ENUM_ENTRY)
                            // TODO: ClosureAnnotator is not catching companion's closures, so the following reporting never happens. Make it work or drop
                            declaration.isCompanion -> reportError(
                                JvmBackendErrors.SCRIPT_CAPTURING_OBJECT, SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
                            )
                            declaration.kind.isSingleton -> reportError(JvmBackendErrors.SCRIPT_CAPTURING_OBJECT)

                            declaration.isClass ->
                                if (declaration.parent != irScript) {
                                    if ((declaration.parent as? IrClass)?.isInner == false) {
                                        context.ktDiagnosticReporter.at(declaration).report(
                                            JvmBackendErrors.SCRIPT_CAPTURING_NESTED_CLASS,
                                            declaration.name.asString(),
                                            ((declaration.parent as? IrDeclarationWithName)?.name
                                                ?: SpecialNames.NO_NAME_PROVIDED).asString()
                                        )
                                    }
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
        val capturingClasses = collectCapturingClasses(irScript, typeRemapper)

        val earlierScriptField = irScript.earlierScriptsParameter?.let { earlierScriptsParameter ->
            irScriptClass.factory.createField(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.SCRIPT_EARLIER_SCRIPTS,
                IrFieldSymbolImpl(), Name.identifier("\$\$earlierScripts"), earlierScriptsParameter.type,
                DescriptorVisibilities.PRIVATE, isFinal = true, isExternal = false, isStatic = false
            )
        }?.also {
            it.parent = irScriptClass
            irScriptClass.declarations.add(it)
        }

        val implicitReceiversFieldsWithParameters = arrayListOf<Pair<IrField, IrValueParameter>>().apply {
            irScript.implicitReceiversParameters.forEach { param ->
                val field = irScriptClass.factory.createField(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.SCRIPT_IMPLICIT_RECEIVER,
                    IrFieldSymbolImpl(), Name.identifier("\$\$implicitReceiver_${param.type.classFqName?.shortName()?.asString()!!}"),
                    typeRemapper.remapType(param.type),
                    DescriptorVisibilities.PRIVATE, isFinal = true, isExternal = false, isStatic = false
                )
                field.parent = irScriptClass
                irScriptClass.declarations.add(field)
                add(field to param)
            }
        }

        val scriptTransformer = ScriptToClassTransformer(
            irScript,
            irScriptClass,
            typeRemapper,
            context,
            capturingClasses,
            innerClassesSupport,
            earlierScriptField,
            implicitReceiversFieldsWithParameters
        )
        val lambdaPatcher = ScriptFixLambdasTransformer(irScriptClass)

        irScriptClass.thisReceiver = scriptTransformer.scriptClassReceiver

        val defaultContext = ScriptToClassTransformerContext(
            valueParameterForScriptThis = irScriptClass.thisReceiver?.symbol,
            fieldForScriptThis = null,
            valueParameterForFieldReceiver = null,
            isInScriptConstructor = false
        )

        fun <E : IrElement> E.patchForClass(): IrElement =
            transform(
                scriptTransformer,
                (this as? IrDeclaration)?.let { defaultContext.copy( topLevelDeclaration = it) } ?: defaultContext
            ).transform(lambdaPatcher, ScriptFixLambdasTransformerContext())

        (irScript.constructor?.patchForClass() as? IrConstructor ?: createConstructor(irScriptClass, irScript)).also { constructor ->
            val explicitParamsStartIndex = if (irScript.earlierScriptsParameter == null) 0 else 1
            val explicitParameters = constructor.valueParameters.subList(
                explicitParamsStartIndex,
                irScript.explicitCallParameters.size + explicitParamsStartIndex
            )
            constructor.body = context.createIrBuilder(constructor.symbol).irBlockBody {
                val baseClassCtor = irScript.baseClass?.classOrNull?.owner?.constructors?.firstOrNull()
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
                if (earlierScriptField != null) {
                    +irSetField(irGet(irScriptClass.thisReceiver!!), earlierScriptField, irGet(irScript.earlierScriptsParameter!!))
                }
                implicitReceiversFieldsWithParameters.forEach { (field, correspondingParameter) ->
                    +irSetField(
                        irGet(irScriptClass.thisReceiver!!),
                        field,
                        irGet(correspondingParameter.patchForClass() as IrValueParameter)
                    )
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
            context.state.scriptSpecific.resultType = irResultProperty.backingField?.type?.toIrBasedKotlinType()
        }
    }

    private fun createConstructor(
        irScriptClass: IrClass,
        irScript: IrScript
    ): IrConstructor =
        with(IrFunctionBuilder().apply {
            isPrimary = true
            returnType = irScriptClass.thisReceiver!!.type as IrSimpleType
        }) {
            irScriptClass.factory.createConstructor(
                startOffset, endOffset, origin,
                IrConstructorSymbolImpl(),
                SpecialNames.INIT,
                visibility, returnType,
                isInline = isInline, isExternal = isExternal, isPrimary = isPrimary, isExpect = isExpect,
                containerSource = containerSource
            )
        }.also { irConstructor ->
            irConstructor.valueParameters = buildList {
                addIfNotNull(irScript.earlierScriptsParameter)
                addAll(irScript.explicitCallParameters.map {
                    IrValueParameterImpl(
                        it.startOffset, it.endOffset,
                        IrDeclarationOrigin.SCRIPT_CALL_PARAMETER, IrValueParameterSymbolImpl(),
                        it.name, index = 0,
                        type = it.type, varargElementType = null,
                        isCrossinline = false, isNoinline = false, isHidden = false, isAssignable = false
                    ).also { it.parent = irScript }
                })
                addAll(irScript.implicitReceiversParameters)
                addAll(irScript.providedPropertiesParameters)
            }
            irConstructor.parent = irScript
        }

    private val scriptingJvmPackage by lazy(LazyThreadSafetyMode.PUBLICATION) {
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(context.state.module, FqName("kotlin.script.experimental.jvm"))
    }

    private fun IrClass.addScriptMainFun() {
        val javaLangClass = context.ir.symbols.javaLangClass
        val kClassJavaPropertyGetter = context.ir.symbols.kClassJavaPropertyGetter

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
                            irGet(javaLangClass.starProjectedType, null, kClassJavaPropertyGetter.symbol).apply {
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
    val valueParameterForFieldReceiver: IrValueParameterSymbol?,
    val isInScriptConstructor: Boolean,
    val topLevelDeclaration: IrDeclaration? = null
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
    val innerClassesSupport: JvmInnerClassesSupport,
    val earlierScriptsField: IrField?,
    val implicitReceiversFieldsWithParameters: Collection<Pair<IrField, IrValueParameter>>
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
        irScript.thisReceiver?.transform(this, ScriptToClassTransformerContext(null, null, null, false)) ?: run {
            context.symbolTable.enterScope(irScriptClass)
            val thisType = IrSimpleTypeImpl(irScriptClass.symbol, false, emptyList(), emptyList())
            val newReceiver = irScriptClass.createThisReceiverParameter(IrDeclarationOrigin.INSTANCE_RECEIVER, thisType)
            context.symbolTable.leaveScope(irScriptClass)
            newReceiver
        }

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
                val newDispatchReceiverParameter = dispatchReceiverParameter?.transform(data) ?: run {
                    if (this.isCurrentScriptTopLevelDeclaration(data)) {
                        createThisReceiverParameter(IrDeclarationOrigin.SCRIPT_THIS_RECEIVER, scriptClassReceiver.type)
                    } else null
                }
                val isInScriptConstructor = this@transformFunctionChildren is IrConstructor && parent == irScript
                val dataForChildren =
                    when {
                        newDispatchReceiverParameter == null -> data

                        newDispatchReceiverParameter.type == scriptClassReceiver.type ->
                            ScriptToClassTransformerContext(newDispatchReceiverParameter.symbol, null, null, isInScriptConstructor)

                        newDispatchReceiverParameter.type == data.valueParameterForFieldReceiver?.owner?.type ->
                            ScriptToClassTransformerContext(
                                null,
                                data.fieldForScriptThis,
                                newDispatchReceiverParameter.symbol,
                                isInScriptConstructor
                            )

                        else -> data
                    }
                dispatchReceiverParameter = newDispatchReceiverParameter
                extensionReceiverParameter = extensionReceiverParameter?.transform(dataForChildren)
                returnType = returnType.remapType()
                valueParameters = valueParameters.transform(dataForChildren)
                body = body?.transform(dataForChildren)
            }
        }

    private fun IrDeclarationParent.createThisReceiverParameter(origin: IrDeclarationOrigin, type: IrType): IrValueParameter =
        context.symbolTable.irFactory.createValueParameter(
            startOffset, endOffset, origin, IrValueParameterSymbolImpl(),
            SpecialNames.THIS, UNDEFINED_PARAMETER_INDEX, type,
            varargElementType = null, isCrossinline = false, isNoinline = false,
            isHidden = false, isAssignable = false
        ).also {
            it.parent = this
        }

    private fun IrTypeParameter.remapSuperTypes(): IrTypeParameter = apply {
        superTypes = superTypes.map { it.remapType() }
    }

    private fun unexpectedElement(element: IrElement): Nothing =
        throw IllegalArgumentException("Unsupported element type: $element")

    override fun visitElement(element: IrElement, data: ScriptToClassTransformerContext): IrElement = unexpectedElement(element)

    override fun visitModuleFragment(declaration: IrModuleFragment, data: ScriptToClassTransformerContext): IrModuleFragment =
        unexpectedElement(declaration)

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: ScriptToClassTransformerContext) =
        unexpectedElement(declaration)

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
                        null, innerClassesSupport.getOuterThisField(it).symbol, it.thisReceiver?.symbol, false
                    )
            }
        }
        transformAnnotations(dataForChildren)
        transformChildren(this@ScriptToClassTransformer, dataForChildren)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: ScriptToClassTransformerContext): IrSimpleFunction =
        declaration.apply {
            transformParent()
            transformFunctionChildren(data)
        }

    override fun visitConstructor(declaration: IrConstructor, data: ScriptToClassTransformerContext): IrConstructor = declaration.apply {
        if (declaration in capturingClassesConstructors) {
            declaration.dispatchReceiverParameter =
                declaration.createThisReceiverParameter(IrDeclarationOrigin.INSTANCE_RECEIVER, scriptClassReceiver.type)
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

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: ScriptToClassTransformerContext): IrExpression {
        for (i in 0 until expression.typeArgumentsCount) {
            expression.putTypeArgument(i, expression.getTypeArgument(i)?.remapType())
        }
        if (expression.dispatchReceiver == null && (expression.symbol.owner as? IrDeclaration)?.needsScriptReceiver() == true) {
            expression.dispatchReceiver =
                getAccessCallForScriptInstance(
                    data, expression.startOffset, expression.endOffset, expression.origin, originalReceiverParameter = null
                )
        }
        return super.visitMemberAccess(expression, data) as IrExpression
    }

    override fun visitGetField(expression: IrGetField, data: ScriptToClassTransformerContext): IrExpression {
        if (expression.receiver == null && expression.symbol.owner.needsScriptReceiver()) {
            expression.receiver =
                getAccessCallForScriptInstance(
                    data, expression.startOffset, expression.endOffset, expression.origin, originalReceiverParameter = null
                )
        }
        return super.visitGetField(expression, data)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: ScriptToClassTransformerContext): IrExpression {
        if (expression.dispatchReceiver == null) {
            // first part of the expression triggers if the ctor itself is transformed before call
            // but the second part is not enough by itself if capturing class is defined in an earlier snippet (we are not keeping
            // capturing classes from earlier snippets)
            val ctorDispatchReceiverType = expression.symbol.owner.dispatchReceiverParameter?.type
                ?: if (capturingClassesConstructors.keys.any { it.symbol == expression.symbol }) scriptClassReceiver.type else null
            if (ctorDispatchReceiverType != null) {
                getDispatchReceiverExpression(data, expression, ctorDispatchReceiverType, expression.origin, null)?.let {
                    expression.dispatchReceiver = it
                }
            }
        }
        return super.visitConstructorCall(expression, data) as IrExpression
    }

    private fun getDispatchReceiverExpression(
        data: ScriptToClassTransformerContext,
        expression: IrDeclarationReference,
        receiverType: IrType,
        origin: IrStatementOrigin?,
        originalReceiverParameter: IrValueParameter?,
    ): IrExpression? {
        return if (receiverType == scriptClassReceiver.type) {
            getAccessCallForScriptInstance(data, expression.startOffset, expression.endOffset, origin, originalReceiverParameter)
        } else {
            getAccessCallForImplicitReceiver(data, expression, receiverType, origin, originalReceiverParameter)
        }
    }

    private fun getAccessCallForScriptInstance(
        data: ScriptToClassTransformerContext,
        startOffset: Int,
        endOffset: Int,
        origin: IrStatementOrigin?,
        originalReceiverParameter: IrValueParameter?
    ): IrExpression? = when {
        // do not touch receiver of a different type
        originalReceiverParameter != null && originalReceiverParameter.type != scriptClassReceiver.type ->
            null

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

    private fun getAccessCallForImplicitReceiver(
        data: ScriptToClassTransformerContext,
        expression: IrDeclarationReference,
        receiverType: IrType,
        expressionOrigin: IrStatementOrigin?,
        originalReceiverParameter: IrValueParameter?
    ): IrExpression? {
        // implicit receivers has priority (as per descriptor outer scopes)
        implicitReceiversFieldsWithParameters.firstOrNull {
            if (originalReceiverParameter != null) it.second == originalReceiverParameter
            else it.second.type == receiverType
        }?.let { (field, param) ->
            val builder = context.createIrBuilder(expression.symbol)
            return if (data.isInScriptConstructor) {
                builder.irGet(param.type, param.symbol)
            } else {
                val scriptReceiver =
                    getAccessCallForScriptInstance(data, expression.startOffset, expression.endOffset, expressionOrigin, null)
                builder.irGetField(scriptReceiver, field)
            }
        }
        // earlier scripts are processed next
        return getAccessCallForEarlierScript(data, expression, receiverType, expressionOrigin)
    }

    private fun getAccessCallForEarlierScript(
        data: ScriptToClassTransformerContext,
        expression: IrDeclarationReference,
        maybeScriptType: IrType,
        expressionOrigin: IrStatementOrigin?
    ): IrExpression? {
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

            val irGetEarlierScripts =
                if (data.isInScriptConstructor) {
                    builder.irGet(objArray.defaultType, irScript.earlierScriptsParameter!!.symbol)
                } else {
                    val scriptReceiver =
                        getAccessCallForScriptInstance(data, expression.startOffset, expression.endOffset, expressionOrigin, null)
                    builder.irGetField(scriptReceiver, earlierScriptsField!!)
                }
            val getPrevScriptObjectExpression = builder.irCall(objArrayGet).apply {
                dispatchReceiver = irGetEarlierScripts
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

    override fun visitGetValue(expression: IrGetValue, data: ScriptToClassTransformerContext): IrExpression {
        val getVar = expression.symbol.owner as? IrVariable
        if (getVar != null) {
            if (irScript.explicitCallParameters.contains(getVar)) {
                val correspondingParam = irScriptClass.constructors.single().valueParameters.find {
                    it.origin == IrDeclarationOrigin.SCRIPT_CALL_PARAMETER && it.name == getVar.name
                } ?: error("script explicit parameter ${getVar.name.asString()} not found")
                val newExpression =
                    IrGetValueImpl(
                        expression.startOffset, expression.endOffset,
                        correspondingParam.type, correspondingParam.symbol,
                        expression.origin
                    )
                return super.visitExpression(newExpression, data)
            }
        } else if (irScript.needsReceiverProcessing) {
            val getValueParameter = expression.symbol.owner as? IrValueParameter
            if (getValueParameter != null && getValueParameter.name == SpecialNames.THIS) {
                val newExpression = getDispatchReceiverExpression(
                    data, expression, getValueParameter.type, expression.origin, getValueParameter
                )
                if (newExpression != null) {
                    return super.visitExpression(newExpression, data)
                }
            }
        }
        return super.visitGetValue(expression, data)
    }

    private fun IrDeclaration.isCurrentScriptTopLevelDeclaration(data: ScriptToClassTransformerContext): Boolean {
        if (data.topLevelDeclaration == null || (parent != irScript && parent != irScriptClass)) return false
        val declarationToCompare = if (this is IrFunction) this.propertyIfAccessor else this
        // TODO: might be fragile, if we'll start to use transformed declaration on either side, try to find a way to detect or avoid
        return declarationToCompare == data.topLevelDeclaration
    }

    private fun IrDeclaration.needsScriptReceiver() =
        (this as? IrFunction)?.dispatchReceiverParameter?.origin == IrDeclarationOrigin.SCRIPT_THIS_RECEIVER
}

private class ScriptFixLambdasTransformer(val irScriptClass: IrClass) : IrElementTransformer<ScriptFixLambdasTransformerContext> {

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

private val IrScript.needsReceiverProcessing: Boolean
    // in K2 we need to add dispatch receiver to the top-level declarations, and in all cases receivers should be replaced
    // for all kinds of implicit receivers
    get() = origin == SCRIPT_K2_ORIGIN || earlierScripts?.isNotEmpty() == true || implicitReceiversParameters.isNotEmpty()