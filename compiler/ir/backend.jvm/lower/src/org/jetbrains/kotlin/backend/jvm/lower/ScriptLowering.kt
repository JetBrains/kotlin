/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.scripting.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.SCRIPT_K2_ORIGIN
import org.jetbrains.kotlin.ir.descriptors.toIrBasedKotlinType
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.topologicalSort

@PhaseDescription(name = "ScriptsToClasses")
internal class ScriptsToClassesLowering(val context: JvmBackendContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        val scripts = mutableListOf<IrScript>()
        val scriptDependencies = mutableMapOf<IrScript, List<IrScript>>()

        for (irFile in irModule.files) {
            val iterator = irFile.declarations.listIterator()
            while (iterator.hasNext()) {
                val declaration = iterator.next()
                if (declaration is IrScript) {
                    val scriptClass = prepareScriptClass(irFile, declaration)
                    scripts.add(declaration)
                    declaration.importedScripts.takeUnless { it.isNullOrEmpty() }?.let {
                        scriptDependencies[declaration] = it.map { it.owner }
                    }
                    iterator.set(scriptClass)
                }
            }
        }

        val symbolRemapper = ScriptsToClassesSymbolRemapper()

        val orderedScripts = topologicalSort(scripts) { scriptDependencies[this] ?: emptyList() }.reversed()
        for (irScript in orderedScripts) {
            finalizeScriptClass(irScript, symbolRemapper)
            // TODO fix parents in script classes
            irScript.targetClass!!.owner.patchDeclarationParents(irScript.parent)
        }
    }

    private fun prepareScriptClass(irFile: IrFile, irScript: IrScript): IrClass {
        val fileEntry = irFile.fileEntry
        return context.irFactory.buildClass {
            startOffset = 0
            endOffset = fileEntry.maxOffset
            origin = IrDeclarationOrigin.SCRIPT_CLASS
            name = NameUtils.getScriptTargetClassName(irScript.name)
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
        val scriptsReceivers = mutableSetOf<IrType>().also {
            it.addIfNotNull(irScript.thisReceiver?.type)
        }
        irScript.earlierScripts?.forEach { scriptsReceivers.addIfNotNull(it.owner.thisReceiver?.type) }
        irScript.importedScripts?.forEach {
            scriptsReceivers.add(it.owner.targetClass!!.owner.thisReceiver!!.type)
        }
        irScript.implicitReceiversParameters.forEach {
            scriptsReceivers.add(it.type)
            scriptsReceivers.add(typeRemapper.remapType(it.type))
        }

        return irScript.statements.filterIsInstance<IrClass>().collectCapturersByReceivers(context, irScript, scriptsReceivers)
    }

    private fun finalizeScriptClass(irScript: IrScript, symbolRemapper: ScriptsToClassesSymbolRemapper) {

        if (irScript.thisReceiver == null) {
            // This is a placeholder transformed to a proper receiver for script class down below, but it is necessary for
            // collecting captured script instances (see ClosureAnnotator.ClosureCollectorVisitor.processScriptCapturing)
            val type = IrSimpleTypeImpl(irScript.symbol, false, emptyList(), emptyList())
            irScript.thisReceiver = irScript.createThisReceiverParameter(context, IrDeclarationOrigin.INSTANCE_RECEIVER, type)
        }

        val irScriptClass = irScript.targetClass!!.owner
        val typeRemapper = SimpleTypeRemapper(symbolRemapper)
        val capturingClasses = collectCapturingClasses(irScript, typeRemapper)

        val earlierScriptField = irScriptClass.addEarlierScriptField(irScript)

        val implicitReceiversFieldsWithParameters = makeImplicitReceiversFieldsWithParameters(irScriptClass, typeRemapper, irScript)

        val targetClassReceiver: IrValueParameter =
            irScript.thisReceiver!!.also {
                it.type = IrSimpleTypeImpl(irScriptClass.symbol, false, emptyList(), emptyList())
            }

        val accessCallsGenerator =
            ScriptAccessCallsGenerator(
                context, targetClassReceiver, implicitReceiversFieldsWithParameters, irScript, earlierScriptField
            )

        val scriptTransformer = ScriptToClassTransformer(
            context,
            irScript,
            irScriptClass,
            targetClassReceiver,
            typeRemapper,
            accessCallsGenerator,
            capturingClasses,
        )

        val lambdaPatcher = ScriptFixLambdasTransformer(irScriptClass)

        patchDeclarationsDispatchReceiver(irScript.statements, context, targetClassReceiver.type)

        irScriptClass.thisReceiver = targetClassReceiver

        fun <E : IrElement> E.patchDeclarationForClass(): IrElement {
            val rootContext =
                ScriptLikeToClassTransformerContext.makeRootContext(
                    irScriptClass.thisReceiver?.symbol, isInScriptConstructor = false, this as? IrDeclaration
                )
            return transform(scriptTransformer, rootContext)
                .transform(lambdaPatcher, ScriptFixLambdasTransformerContext())
        }

        fun <E : IrElement> E.patchTopLevelStatementForClass(): IrElement {
            val rootContext =
                ScriptLikeToClassTransformerContext.makeRootContext(irScriptClass.thisReceiver?.symbol, isInScriptConstructor = true)
            return transform(scriptTransformer, rootContext)
                .transform(lambdaPatcher, ScriptFixLambdasTransformerContext())
        }

        val explicitParametersWithFields = irScript.explicitCallParameters.map { parameter ->
            val field = irScriptClass.addField {
                startOffset = parameter.startOffset
                endOffset = parameter.endOffset
                origin = IrDeclarationOrigin.SCRIPT_CALL_PARAMETER
                name = parameter.name
                type = parameter.type
                visibility = DescriptorVisibilities.LOCAL
                isFinal = true
            }
            parameter to field
        }

        (irScript.constructor?.patchDeclarationForClass() as? IrConstructor
            ?: createConstructor(irScriptClass, irScript, implicitReceiversFieldsWithParameters)).also { constructor ->
            val explicitParamsStartIndex = if (irScript.earlierScriptsParameter == null) 0 else 1
            val explicitParameters = constructor.valueParameters.subList(
                explicitParamsStartIndex,
                irScript.explicitCallParameters.size + explicitParamsStartIndex
            )
            constructor.body =
                context.createIrBuilder(constructor.symbol)
                    .makeScriptClassConstructorBody(
                        irScript,
                        irScriptClass,
                        explicitParameters,
                        earlierScriptField,
                        explicitParametersWithFields,
                        implicitReceiversFieldsWithParameters
                    )
            irScriptClass.declarations.add(constructor)
            constructor.parent = irScriptClass
        }

        var hasMain = false
        irScript.statements.forEach { scriptStatement ->
            when (scriptStatement) {
                is IrVariable -> {
                    val copy = scriptStatement.patchDeclarationForClass() as IrVariable
                    irScriptClass.addSimplePropertyFrom(copy)
                }
                is IrDeclaration -> {
                    val copy = scriptStatement.patchDeclarationForClass() as IrDeclaration
                    irScriptClass.declarations.add(copy)
                    // temporary way to avoid name clashes
                    // TODO: remove as soon as main generation become an explicit configuration option
                    if (copy is IrSimpleFunction && copy.name.asString() == "main") {
                        hasMain = true
                    }
                }
                else -> {
                    val transformedStatement = scriptStatement.patchTopLevelStatementForClass() as IrStatement
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
        irScript: IrScript,
        implicitReceiversFieldsWithParameters: ArrayList<Pair<IrField, IrValueParameter>>
    ): IrConstructor =
        with(IrFunctionBuilder().apply {
            isPrimary = true
            returnType = irScriptClass.thisReceiver!!.type as IrSimpleType
        }) {
            irScriptClass.factory.createConstructor(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = SpecialNames.INIT,
                visibility = visibility,
                isInline = isInline,
                isExpect = isExpect,
                returnType = returnType,
                symbol = IrConstructorSymbolImpl(),
                isPrimary = isPrimary,
                isExternal = isExternal,
                containerSource = containerSource,
            )
        }.also { irConstructor ->
            irConstructor.valueParameters = buildList {
                irScript.earlierScriptsParameter?.let {
                    add(it)
                }
                addAll(
                    irScript.explicitCallParameters.map {
                        context.irFactory.createValueParameter(
                            startOffset = it.startOffset,
                            endOffset = it.endOffset,
                            origin = IrDeclarationOrigin.SCRIPT_CALL_PARAMETER,
                            name = it.name,
                            type = it.type,
                            isAssignable = false,
                            symbol = IrValueParameterSymbolImpl(),
                            varargElementType = null,
                            isCrossinline = false,
                            isNoinline = false,
                            isHidden = false,
                        ).also { it.parent = irScript }
                    },
                )
                implicitReceiversFieldsWithParameters.forEach {(_, param) ->
                    add(param)
                }
                addAll(irScript.providedPropertiesParameters)
            }
            irConstructor.parent = irScript
        }

    private val scriptingJvmPackage by lazy(LazyThreadSafetyMode.PUBLICATION) {
        createEmptyExternalPackageFragment(context.state.module, FqName("kotlin.script.experimental.jvm"))
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
            createThisReceiverParameter()
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
            }

            property.addDefaultGetter(this, context.irBuiltIns)
        }
    }
}

private fun IrBuilderWithScope.makeScriptClassConstructorBody(
    irScript: IrScript,
    irScriptClass: IrClass,
    explicitParameters: List<IrValueParameter>,
    earlierScriptField: IrField?,
    explicitParametersWithFields: List<Pair<IrVariable, IrField>>,
    implicitReceiversFieldsWithParameters: java.util.ArrayList<Pair<IrField, IrValueParameter>>,
) = irBlockBody {
    val baseClassCtor = irScript.baseClass?.classOrNull?.owner?.constructors?.firstOrNull()
    // TODO: process situation with multiple constructors (should probably be an error)
    if (baseClassCtor == null) {
        +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
    } else {
        +irDelegatingConstructorCall(baseClassCtor).also {
            explicitParameters.forEachIndexed { idx, valueParameter ->
                // Since in K2 we're not distinguishing between base class ctor args and other script args, we need this check.
                // The logic is fragile, but since we plan to deprecate baseClass support (see KT-60449), and this delegating
                // call will go away, let's leave it as is for now
                if (idx >= it.valueArgumentsCount) return@forEachIndexed
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
    explicitParametersWithFields.forEach { (parameter, field) ->
        +irSetField(
            irGet(irScriptClass.thisReceiver!!),
            field, irGet(parameter.type, explicitParameters.find { it.name == parameter.name }!!.symbol)
        )
    }
    implicitReceiversFieldsWithParameters.forEach { (field, correspondingParameter) ->
        +irSetField(
            irGet(irScriptClass.thisReceiver!!),
            field,
            irGet(correspondingParameter)
        )
    }
    +IrInstanceInitializerCallImpl(
        irScript.startOffset, irScript.endOffset,
        irScriptClass.symbol,
        context.irBuiltIns.unitType
    )
}

private fun makeImplicitReceiversFieldsWithParameters(irScriptClass: IrClass, typeRemapper: SimpleTypeRemapper, irScript: IrScript) =
    arrayListOf<Pair<IrField, IrValueParameter>>().apply {

        fun createField(name: Name, type: IrType): IrField {
            val field = irScriptClass.factory.createField(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.SCRIPT_IMPLICIT_RECEIVER,
                name = name,
                visibility = DescriptorVisibilities.PRIVATE,
                symbol = IrFieldSymbolImpl(),
                type = typeRemapper.remapType(type),
                isFinal = true,
                isStatic = false,
                isExternal = false
            )
            field.parent = irScriptClass
            irScriptClass.declarations.add(field)
            return field
        }

        irScript.importedScripts?.forEach {
            val importedScriptClass = it.owner.targetClass!!.owner
            val type = importedScriptClass.defaultType
            val name = Name.identifier("\$\$importedScript_${type.classFqName?.shortName()?.asString()!!}")
            val param = irScriptClass.factory.createValueParameter(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.SCRIPT_IMPLICIT_RECEIVER, name, type, isAssignable = false,
                IrValueParameterSymbolImpl(), varargElementType = null,
                isCrossinline = false, isNoinline = false, isHidden = false,
            )
            param.parent = irScriptClass
            add(createField(name, type) to param)
        }
        irScript.implicitReceiversParameters.forEach { param ->
            val typeName = param.type.classFqName?.shortName()?.identifierOrNullIfSpecial
            add(
                createField(
                    Name.identifier("\$\$implicitReceiver_${typeName ?: param.indexInOldValueParameters.toString()}"),
                    param.type
                ) to param
            )
        }
    }

private class ScriptAccessCallsGenerator(
    context: JvmBackendContext,
    targetClassReceiver: IrValueParameter,
    implicitReceiversFieldsWithParameters: ArrayList<Pair<IrField, IrValueParameter>>,
    val irScript: IrScript,
    val earlierScriptsField: IrField?,
) : ScriptLikeAccessCallsGenerator(context, targetClassReceiver, implicitReceiversFieldsWithParameters) {

    override fun getAccessCallForImplicitReceiver(
        data: ScriptLikeToClassTransformerContext,
        expression: IrDeclarationReference,
        receiverType: IrType,
        expressionOrigin: IrStatementOrigin?,
        originalReceiverParameter: IrValueParameter?,
    ): IrExpression? =
        super.getAccessCallForImplicitReceiver(data, expression, receiverType, expressionOrigin, originalReceiverParameter)
            ?: getAccessCallForEarlierScript(data, expression, receiverType, expressionOrigin)

    private fun getAccessCallForEarlierScript(
        data: ScriptLikeToClassTransformerContext,
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
                        getAccessCallForSelf(data, expression.startOffset, expression.endOffset, expressionOrigin, null)
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

}

private class ScriptToClassTransformer(
    context: JvmBackendContext,
    val irScript: IrScript,
    irScriptClass: IrClass,
    targetClassReceiver: IrValueParameter,
    typeRemapper: TypeRemapper,
    accessCallsGenerator: ScriptLikeAccessCallsGenerator,
    capturingClasses: Set<IrClassImpl>,
) : ScriptLikeToClassTransformer(
    context,
    irScript,
    irScriptClass,
    targetClassReceiver,
    typeRemapper,
    accessCallsGenerator,
    capturingClasses,
    needsReceiverProcessing = irScript.needsReceiverProcessing
) {
    override fun visitGetValue(expression: IrGetValue, data: ScriptLikeToClassTransformerContext): IrExpression {
        val correspondingVariable = expression.symbol.owner as? IrVariable
        return if (correspondingVariable != null && irScript.explicitCallParameters.contains(correspondingVariable)) {
            val builder = context.createIrBuilder(expression.symbol)
            val newExpression =
                if (data.isInScriptConstructor) {
                    val correspondingCtorParam = irTargetClass.constructors.single().valueParameters.find {
                        it.origin == IrDeclarationOrigin.SCRIPT_CALL_PARAMETER && it.name == correspondingVariable.name
                    } ?: error("script explicit parameter ${correspondingVariable.name.asString()} not found")
                    builder.irGet(correspondingCtorParam.type, correspondingCtorParam.symbol)
                } else {
                    val correspondingField = irTargetClass.declarations.find {
                        it is IrField && it.origin == IrDeclarationOrigin.SCRIPT_CALL_PARAMETER && it.name == correspondingVariable.name
                    } ?: error("script explicit parameter ${correspondingVariable.name.asString()} corresponding property not found")
                    val scriptReceiver =
                        accessCallsGenerator.getAccessCallForSelf(
                            data, expression.startOffset, expression.endOffset, expression.origin, null
                        )
                    builder.irGetField(scriptReceiver, correspondingField as IrField)
                }
            super.visitExpression(newExpression, data)
        } else {
            super.visitGetValue(expression, data)
        }
    }

    override fun isValidNameForReceiver(name: Name) =
        super.isValidNameForReceiver(name) || irScript.implicitReceiversParameters.any { it.name == name }
}

private class ScriptsToClassesSymbolRemapper : SymbolRemapper.Empty() {
    override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol =
        (symbol.owner as? IrScript)?.targetClass  ?: symbol
}

private fun IrClass.addEarlierScriptField(irScript: IrScript) =
    irScript.earlierScriptsParameter?.let { earlierScriptsParameter ->
        factory.createField(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.SCRIPT_EARLIER_SCRIPTS,
            name = Name.identifier("\$\$earlierScripts"),
            visibility = DescriptorVisibilities.PRIVATE,
            symbol = IrFieldSymbolImpl(),
            type = earlierScriptsParameter.type,
            isFinal = true,
            isStatic = false,
            isExternal = false,
        )
    }?.also {
        it.parent = this
        declarations.add(it)
    }

private inline fun IrClass.addAnonymousInitializer(builder: IrFunctionBuilder.() -> Unit = {}): IrAnonymousInitializer =
    IrFunctionBuilder().run {
        builder()
        returnType = defaultType
        factory.createAnonymousInitializer(
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
    get() = origin == SCRIPT_K2_ORIGIN || importedScripts?.isNotEmpty() == true
            || earlierScripts?.isNotEmpty() == true || implicitReceiversParameters.isNotEmpty()

