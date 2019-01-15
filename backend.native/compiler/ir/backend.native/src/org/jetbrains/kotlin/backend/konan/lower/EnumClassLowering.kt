/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.backend.common.ir.addSimpleDelegatingConstructor
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_ENUM
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.irasdescriptors.constructedClass
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal class EnumSyntheticFunctionsBuilder(val context: Context) {
    fun buildValuesExpression(startOffset: Int, endOffset: Int,
                              enumClass: IrClass): IrExpression {

        val loweredEnum = context.specialDeclarationsFactory.getLoweredEnum(enumClass)

        return irCall(startOffset, endOffset, genericValuesSymbol.owner, listOf(enumClass.defaultType))
                .apply {
                    val receiver = IrGetObjectValueImpl(startOffset, endOffset,
                            loweredEnum.implObject.defaultType,
                            loweredEnum.implObject.symbol)
                    putValueArgument(0, IrGetFieldImpl(
                            startOffset,
                            endOffset,
                            loweredEnum.valuesField.symbol,
                            loweredEnum.valuesField.type,
                            receiver
                    ))
                }
    }

    fun buildValueOfExpression(startOffset: Int, endOffset: Int,
                               enumClass: IrClass,
                               value: IrExpression): IrExpression {
        val loweredEnum = context.specialDeclarationsFactory.getLoweredEnum(enumClass)

        return irCall(startOffset, endOffset, genericValueOfSymbol.owner, listOf(enumClass.defaultType))
                .apply {
                    putValueArgument(0, value)
                    val receiver = IrGetObjectValueImpl(startOffset, endOffset,
                            loweredEnum.implObject.defaultType, loweredEnum.implObject.symbol)
                    putValueArgument(1, IrGetFieldImpl(
                            startOffset,
                            endOffset,
                            loweredEnum.valuesField.symbol,
                            loweredEnum.valuesField.type,
                            receiver
                    ))
                }
    }

    private val genericValueOfSymbol = context.ir.symbols.valueOfForEnum

    private val genericValuesSymbol = context.ir.symbols.valuesForEnum
}

internal class EnumUsageLowering(val context: Context)
    : IrElementTransformerVoid(), FileLoweringPass {

    private val enumSyntheticFunctionsBuilder = EnumSyntheticFunctionsBuilder(context)

    override fun lower(irFile: IrFile) {
        visitFile(irFile)
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue): IrExpression {
        val entry = expression.symbol.owner
        return loadEnumEntry(
                expression.startOffset,
                expression.endOffset,
                entry.parentAsClass,
                entry.name
        )
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)

        if (expression.symbol != enumValuesSymbol && expression.symbol != enumValueOfSymbol)
            return expression

        val irClassSymbol = expression.getTypeArgument(0)!!.classifierOrNull as? IrClassSymbol
                ?: return expression // Type parameter.

        if (irClassSymbol == context.ir.symbols.enum) return expression // Type parameter erased to 'Enum'.

        val irClass = irClassSymbol.owner

        assert (irClass.kind == ClassKind.ENUM_CLASS)

        return if (expression.symbol == enumValuesSymbol) {
            enumSyntheticFunctionsBuilder.buildValuesExpression(expression.startOffset, expression.endOffset, irClass)
        } else {
            val value = expression.getValueArgument(0)!!
            enumSyntheticFunctionsBuilder.buildValueOfExpression(expression.startOffset, expression.endOffset, irClass, value)
        }
    }

    private val enumValueOfSymbol = context.ir.symbols.enumValueOf

    private val enumValuesSymbol = context.ir.symbols.enumValues

    private fun loadEnumEntry(startOffset: Int, endOffset: Int, enumClass: IrClass, name: Name): IrExpression {
        val loweredEnum = context.specialDeclarationsFactory.getLoweredEnum(enumClass)
        val ordinal = loweredEnum.entriesMap[name]!!
        return IrCallImpl(
                startOffset, endOffset, enumClass.defaultType,
                loweredEnum.itemGetterSymbol.owner.symbol, loweredEnum.itemGetterSymbol.descriptor,
                typeArgumentsCount = 0
        ).apply {
            dispatchReceiver = IrCallImpl(startOffset, endOffset, loweredEnum.valuesGetter.returnType, loweredEnum.valuesGetter.symbol)
            putValueArgument(0, IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, ordinal))
        }
    }
}

internal class EnumClassLowering(val context: Context) : ClassLoweringPass {

    fun run(irFile: IrFile) {
        runOnFilePostfix(irFile)
        // EnumWhenLowering should be performed before EnumUsageLowering because
        // the latter performs lowering of IrGetEnumValue
        EnumWhenLowering(context).lower(irFile)
        EnumUsageLowering(context).lower(irFile)
    }

    override fun lower(irClass: IrClass) {
        if (irClass.kind != ClassKind.ENUM_CLASS) return
        EnumClassTransformer(irClass).run()
    }

    private interface EnumConstructorCallTransformer {
        fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression
        fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression
    }

    private inner class EnumClassTransformer(val irClass: IrClass) {
        private val loweredEnum = context.specialDeclarationsFactory.getLoweredEnum(irClass)
        private val loweredEnumConstructors = mutableMapOf<IrConstructor, IrConstructor>()
        private val defaultEnumEntryConstructors = mutableMapOf<IrConstructor, IrConstructor>()
        private val loweredEnumConstructorParameters = mutableMapOf<IrValueParameter, IrValueParameter>()
        private val enumSyntheticFunctionsBuilder = EnumSyntheticFunctionsBuilder(context)

        fun run() {
            insertInstanceInitializerCall()
            lowerEnumConstructors(irClass)
            lowerEnumEntriesClasses()
            val defaultClass = createDefaultClassForEnumEntries()
            lowerEnumClassBody(defaultClass)
            createImplObject()
        }

        private fun insertInstanceInitializerCall() {
            irClass.transformChildrenVoid(object: IrElementTransformerVoid() {
                override fun visitClass(declaration: IrClass): IrStatement {
                    // Skip nested
                    return declaration
                }

                override fun visitConstructor(declaration: IrConstructor): IrStatement {
                    declaration.transformChildrenVoid(this)

                    val blockBody = declaration.body as? IrBlockBody
                            ?: throw AssertionError("Unexpected constructor body: ${declaration.body}")
                    if (blockBody.statements.all { it !is IrInstanceInitializerCall }) {
                        blockBody.statements.transformFlat {
                            if (it is IrEnumConstructorCall)
                                listOf(it, IrInstanceInitializerCallImpl(declaration.startOffset, declaration.startOffset,
                                        irClass.symbol, context.irBuiltIns.unitType))
                            else null
                        }
                    }
                    return declaration
                }
            })
        }

        private fun lowerEnumEntriesClasses() {
            irClass.declarations.transformFlat { declaration ->
                if (declaration is IrEnumEntry) {
                    listOfNotNull(declaration, lowerEnumEntryClass(declaration.correspondingClass))
                } else null
            }
        }

        private fun lowerEnumEntryClass(enumEntryClass: IrClass?): IrClass? {
            if (enumEntryClass == null) return null

            lowerEnumConstructors(enumEntryClass)

            return enumEntryClass
        }

        private fun createDefaultClassForEnumEntries(): IrClass? {
            if (!irClass.declarations.any { it is IrEnumEntry && it.correspondingClass == null }) return null
            val defaultClass = WrappedClassDescriptor().let {
                IrClassImpl(
                        irClass.startOffset, irClass.endOffset,
                        DECLARATION_ORIGIN_ENUM,
                        IrClassSymbolImpl(it),
                        "DEFAULT".synthesizedName,
                        ClassKind.CLASS,
                        Visibilities.PRIVATE,
                        Modality.FINAL,
                        isCompanion = false,
                        isInner = false,
                        isData = false,
                        isExternal = false,
                        isInline = false
                ).apply {
                    it.bind(this)
                    parent = irClass
                    irClass.declarations += this
                    createParameterDeclarations()
                }
            }

            for (superConstructor in irClass.constructors) {
                val constructor = defaultClass.addSimpleDelegatingConstructor(superConstructor, context.irBuiltIns)
                defaultEnumEntryConstructors[superConstructor] = constructor

                for (parameter in constructor.valueParameters) {
                    val defaultValue = superConstructor.valueParameters[parameter.index].defaultValue ?: continue
                    val body = defaultValue.deepCopyWithVariables()
                    body.transformChildrenVoid(ParameterMapper(superConstructor, constructor, false))
                    body.patchDeclarationParents(constructor)
                    parameter.defaultValue = body
                }
            }

            defaultClass.superTypes += irClass.defaultType
            defaultClass.addFakeOverrides()

            return defaultClass
        }

        private fun createImplObject() {
            val implObject = loweredEnum.implObject

            val enumEntries = mutableListOf<IrEnumEntry>()
            var i = 0
            while (i < irClass.declarations.size) {
                val declaration = irClass.declarations[i]
                var delete = false
                when (declaration) {
                    is IrEnumEntry -> {
                        enumEntries.add(declaration)
                        delete = true
                    }
                    is IrFunction -> {
                        val body = declaration.body
                        if (body is IrSyntheticBody) {
                            when (body.kind) {
                                IrSyntheticBodyKind.ENUM_VALUEOF ->
                                    declaration.body = createSyntheticValueOfMethodBody(declaration)
                                IrSyntheticBodyKind.ENUM_VALUES ->
                                    declaration.body = createSyntheticValuesMethodBody(declaration)
                            }
                        }
                    }
                }
                if (delete)
                    irClass.declarations.removeAt(i)
                else
                    ++i
            }

            implObject.declarations += createSyntheticValuesPropertyDeclaration(enumEntries)
            implObject.declarations += createValuesPropertyInitializer(enumEntries)

            irClass.declarations += implObject
        }

        private val createUninitializedInstance = context.ir.symbols.createUninitializedInstance.owner

        private fun createSyntheticValuesPropertyDeclaration(enumEntries: List<IrEnumEntry>): IrPropertyImpl {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset

            val irValuesInitializer = context.createArrayOfExpression(
                    startOffset, endOffset,
                    irClass.defaultType,
                    enumEntries
                            .sortedBy { it.name }
                            .map {
                                val initializer = it.initializerExpression
                                val entryConstructorCall = when {
                                    initializer is IrCall -> initializer

                                    initializer is IrBlock && initializer.origin == ARGUMENTS_REORDERING_FOR_CALL ->
                                        initializer.statements.last() as IrCall

                                    else -> error("Unexpected initializer: $initializer")
                                }
                                val entryClass = (entryConstructorCall.symbol.owner as IrConstructor).constructedClass

                                irCall(startOffset, endOffset,
                                        createUninitializedInstance,
                                        listOf(entryClass.defaultType)
                                )

                            }
            )
            val irField = loweredEnum.valuesField.apply {
                initializer = IrExpressionBodyImpl(startOffset, endOffset, irValuesInitializer)
            }

            val getter = loweredEnum.valuesGetter

            val receiver = IrGetObjectValueImpl(startOffset, endOffset,
                    loweredEnum.implObject.defaultType, loweredEnum.implObject.symbol)
            val value = IrGetFieldImpl(
                    startOffset, endOffset,
                    loweredEnum.valuesField.symbol,
                    loweredEnum.valuesField.type,
                    receiver
            )
            val returnStatement = IrReturnImpl(
                    startOffset, endOffset,
                    context.irBuiltIns.nothingType,
                    loweredEnum.valuesGetter.symbol,
                    value
            )
            getter.body = IrBlockBodyImpl(startOffset, endOffset, listOf(returnStatement))

            return IrPropertyImpl(startOffset, endOffset, DECLARATION_ORIGIN_ENUM,
                    false, loweredEnum.valuesField.descriptor, irField, getter, null).apply {
                parent = loweredEnum.implObject
            }
        }

        private val initInstanceSymbol = context.ir.symbols.initInstance

        private val arrayGetSymbol = context.ir.symbols.array.functions.single { it.owner.name == Name.identifier("get") }

        private val arrayType = context.ir.symbols.array.typeWith(irClass.defaultType)

        private fun createValuesPropertyInitializer(enumEntries: List<IrEnumEntry>): IrAnonymousInitializerImpl {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset

            fun IrBlockBodyBuilder.initInstanceCall(instance: IrCall, constructor: IrCall): IrCall =
                    irCall(initInstanceSymbol).apply {
                        putValueArgument(0, instance)
                        putValueArgument(1, constructor)
                    }

            val implObject = loweredEnum.implObject
            return IrAnonymousInitializerImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_ENUM,
                    IrAnonymousInitializerSymbolImpl(WrappedClassDescriptor())
            ).apply {
                parent = implObject
                body = context.createIrBuilder(symbol, startOffset, endOffset).irBlockBody(irClass) {
                    val receiver = implObject.thisReceiver!!
                    val instances = irTemporary(irGetField(irGet(receiver), loweredEnum.valuesField))
                    enumEntries
                            .sortedBy { it.name }
                            .withIndex()
                            .forEach {
                                val instance = irCall(arrayGetSymbol).apply {
                                    dispatchReceiver = irGet(instances)
                                    putValueArgument(0, irInt(it.index))
                                }
                                val initializer = it.value.initializerExpression!!
                                initializer.patchDeclarationParents(implObject)
                                when {
                                    initializer is IrCall -> +initInstanceCall(instance, initializer)

                                    initializer is IrBlock && initializer.origin == ARGUMENTS_REORDERING_FOR_CALL -> {
                                        val statements = initializer.statements
                                        val constructorCall = statements.last() as IrCall
                                        statements[statements.lastIndex] = initInstanceCall(instance, constructorCall)
                                        +initializer
                                    }

                                    else -> error("Unexpected initializer: $initializer")
                                }
                            }
                    +irCall(this@EnumClassLowering.context.ir.symbols.freeze, listOf(arrayType)).apply {
                        extensionReceiver = irGet(receiver)
                    }
                }
            }
        }

        private fun createSyntheticValuesMethodBody(declaration: IrFunction): IrBody {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val valuesExpression = enumSyntheticFunctionsBuilder.buildValuesExpression(startOffset, endOffset, irClass)

            return IrBlockBodyImpl(startOffset, endOffset).apply {
                statements += IrReturnImpl(
                        startOffset,
                        endOffset,
                        context.irBuiltIns.nothingType,
                        declaration.symbol,
                        valuesExpression
                )
            }
        }

        private fun createSyntheticValueOfMethodBody(declaration: IrFunction): IrBody {
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val parameter = declaration.valueParameters[0]
            val value = IrGetValueImpl(startOffset, endOffset, parameter.type, parameter.symbol)
            val valueOfExpression = enumSyntheticFunctionsBuilder.buildValueOfExpression(startOffset, endOffset, irClass, value)

            return IrBlockBodyImpl(startOffset, endOffset).apply {
                statements += IrReturnImpl(
                        startOffset,
                        endOffset,
                        context.irBuiltIns.nothingType,
                        declaration.symbol,
                        valueOfExpression
                )
            }
        }

        private fun lowerEnumConstructors(irClass: IrClass) {
            irClass.declarations.forEachIndexed { index, declaration ->
                if (declaration is IrConstructor)
                    irClass.declarations[index] = transformEnumConstructor(declaration)
            }
        }

        private fun transformEnumConstructor(enumConstructor: IrConstructor): IrConstructor {
            val loweredEnumConstructor = lowerEnumConstructor(enumConstructor)

            for (parameter in enumConstructor.valueParameters) {
                val defaultValue = parameter.defaultValue ?: continue
                defaultValue.transformChildrenVoid(ParameterMapper(enumConstructor, loweredEnumConstructor, true))
                loweredEnumConstructor.valueParameters[parameter.loweredIndex].defaultValue = defaultValue
                defaultValue.patchDeclarationParents(loweredEnumConstructor)
            }

            return loweredEnumConstructor
        }

        private fun lowerEnumConstructor(constructor: IrConstructor): IrConstructorImpl {
            val startOffset = constructor.startOffset
            val endOffset = constructor.endOffset
            val loweredConstructor = WrappedClassConstructorDescriptor(
                    constructor.descriptor.annotations,
                    constructor.descriptor.source
            ).let {
                IrConstructorImpl(
                        startOffset, endOffset,
                        constructor.origin,
                        IrConstructorSymbolImpl(it),
                        constructor.name,
                        Visibilities.PROTECTED,
                        constructor.returnType,
                        isInline = false,
                        isExternal = false,
                        isPrimary = constructor.isPrimary
                ).apply {
                    it.bind(this)
                    parent = constructor.parent
                    body = constructor.body!! // Will be transformed later.
                }
            }

            fun createSynthesizedValueParameter(index: Int, name: String, type: IrType) =
                    WrappedValueParameterDescriptor().let {
                        IrValueParameterImpl(
                                startOffset, endOffset,
                                DECLARATION_ORIGIN_ENUM,
                                IrValueParameterSymbolImpl(it),
                                Name.identifier(name),
                                index,
                                type,
                                varargElementType = null,
                                isCrossinline = false,
                                isNoinline = false
                        ).apply {
                            it.bind(this)
                            parent = loweredConstructor
                        }
                    }

            loweredConstructor.valueParameters += createSynthesizedValueParameter(0, "name", context.irBuiltIns.stringType)
            loweredConstructor.valueParameters += createSynthesizedValueParameter(1, "ordinal", context.irBuiltIns.intType)
            constructor.valueParameters.mapTo(loweredConstructor.valueParameters) {
                it.copyTo(loweredConstructor, index = it.loweredIndex).apply {
                    loweredEnumConstructorParameters[it] = this
                }
            }

            loweredEnumConstructors[constructor] = loweredConstructor

            return loweredConstructor
        }

        private fun lowerEnumClassBody(defaultClass: IrClass?) {
            irClass.transformChildrenVoid(EnumClassBodyTransformer(defaultClass))
            // Enum entries's classes have already been pulled out to the upper level - clear them up.
            irClass.declarations.filterIsInstance<IrEnumEntry>().forEach { it.correspondingClass = null }
        }

        private inner class InEnumClassConstructor(val enumClassConstructor: IrConstructor) :
                EnumConstructorCallTransformer {
            override fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression {
                val startOffset = enumConstructorCall.startOffset
                val endOffset = enumConstructorCall.endOffset
                val origin = enumConstructorCall.origin

                val result = IrDelegatingConstructorCallImpl(
                        startOffset, endOffset,
                        context.irBuiltIns.unitType,
                        enumConstructorCall.symbol)

                assert(result.symbol.owner.valueParameters.size == 2) {
                    "Enum(String, Int) constructor call expected:\n${result.dump()}"
                }

                val nameParameter = enumClassConstructor.valueParameters.getOrElse(0) {
                    throw AssertionError("No 'name' parameter in enum constructor: $enumClassConstructor")
                }

                val ordinalParameter = enumClassConstructor.valueParameters.getOrElse(1) {
                    throw AssertionError("No 'ordinal' parameter in enum constructor: $enumClassConstructor")
                }

                result.putValueArgument(0,
                        IrGetValueImpl(startOffset, endOffset, nameParameter.type, nameParameter.symbol, origin)
                )
                result.putValueArgument(1,
                        IrGetValueImpl(startOffset, endOffset, ordinalParameter.type, ordinalParameter.symbol, origin)
                )
                return result
            }

            override fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression {
                val startOffset = delegatingConstructorCall.startOffset
                val endOffset = delegatingConstructorCall.endOffset

                val delegatingConstructor = delegatingConstructorCall.symbol.owner
                val loweredDelegatingConstructor = loweredEnumConstructors.getOrElse(delegatingConstructor) {
                    throw AssertionError("Constructor called in enum entry initializer should've been lowered: $delegatingConstructor")
                }

                val result = IrDelegatingConstructorCallImpl(
                        startOffset, endOffset,
                        context.irBuiltIns.unitType,
                        loweredDelegatingConstructor.symbol)

                val firstParameter = enumClassConstructor.valueParameters[0]
                result.putValueArgument(0,
                        IrGetValueImpl(startOffset, endOffset, firstParameter.type, firstParameter.symbol))
                val secondParameter = enumClassConstructor.valueParameters[1]
                result.putValueArgument(1,
                        IrGetValueImpl(startOffset, endOffset, secondParameter.type, secondParameter.symbol))

                delegatingConstructor.valueParameters.forEach {
                    result.putValueArgument(it.loweredIndex, delegatingConstructorCall.getValueArgument(it.index))
                }

                return result
            }
        }

        private abstract inner class InEnumEntry(private val enumEntry: IrEnumEntry) : EnumConstructorCallTransformer {

            override fun transform(enumConstructorCall: IrEnumConstructorCall): IrExpression {
                val name = enumEntry.name.asString()
                val ordinal = context.specialDeclarationsFactory.getEnumEntryOrdinal(enumEntry.descriptor)

                val startOffset = enumConstructorCall.startOffset
                val endOffset = enumConstructorCall.endOffset

                val enumConstructor = enumConstructorCall.symbol.owner
                val loweredConstructor = loweredEnumConstructors.getOrElse(enumConstructor) {
                    throw AssertionError("Constructor called in enum entry initializer should've been lowered: $enumConstructor")
                }

                val result = createConstructorCall(startOffset, endOffset, loweredConstructor.symbol)

                result.putValueArgument(0,
                        IrConstImpl.string(startOffset, endOffset, context.irBuiltIns.stringType, name))
                result.putValueArgument(1,
                        IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, ordinal))

                enumConstructor.valueParameters.forEach {
                    result.putValueArgument(it.loweredIndex, enumConstructorCall.getValueArgument(it.index))
                }

                return result
            }

            override fun transform(delegatingConstructorCall: IrDelegatingConstructorCall): IrExpression {
                throw AssertionError("Unexpected delegating constructor call within enum entry: $enumEntry")
            }

            abstract fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: IrConstructorSymbol): IrMemberAccessExpression
        }

        private inner class InEnumEntryClassConstructor(enumEntry: IrEnumEntry) : InEnumEntry(enumEntry) {
            override fun createConstructorCall(
                    startOffset: Int,
                    endOffset: Int,
                    loweredConstructor: IrConstructorSymbol
            ) = IrDelegatingConstructorCallImpl(
                    startOffset,
                    endOffset,
                    context.irBuiltIns.unitType,
                    loweredConstructor
            )
        }

        private inner class InEnumEntryInitializer(enumEntry: IrEnumEntry) : InEnumEntry(enumEntry) {
            override fun createConstructorCall(startOffset: Int, endOffset: Int, loweredConstructor: IrConstructorSymbol): IrCall {
                val irConstructorSymbol = defaultEnumEntryConstructors[loweredConstructor.owner]?.symbol
                        ?: loweredConstructor
                return IrCallImpl(startOffset, endOffset, irConstructorSymbol.owner.returnType, irConstructorSymbol)
            }
        }

        private inner class EnumClassBodyTransformer(val defaultClass: IrClass?) : IrElementTransformerVoid() {
            private var enumConstructorCallTransformer: EnumConstructorCallTransformer? = null

            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.kind == ClassKind.ENUM_CLASS || declaration == defaultClass)
                    return declaration
                return super.visitClass(declaration)
            }

            override fun visitEnumEntry(declaration: IrEnumEntry): IrStatement {
                assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }

                enumConstructorCallTransformer = InEnumEntryInitializer(declaration)

                declaration.initializerExpression = declaration.initializerExpression?.transform(this, data = null)

                enumConstructorCallTransformer = null

                return declaration
            }

            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                val containingClass = declaration.parentAsClass

                // TODO local (non-enum) class in enum class constructor?
                val previous = enumConstructorCallTransformer

                if (containingClass.kind == ClassKind.ENUM_ENTRY) {
                    assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }
                    val entry = irClass.declarations.filterIsInstance<IrEnumEntry>().single { it.correspondingClass == containingClass }
                    enumConstructorCallTransformer = InEnumEntryClassConstructor(entry)
                } else if (containingClass.kind == ClassKind.ENUM_CLASS) {
                    assert(enumConstructorCallTransformer == null) { "Nested enum entry initialization:\n${declaration.dump()}" }
                    enumConstructorCallTransformer = InEnumClassConstructor(declaration)
                }

                val result = super.visitConstructor(declaration)

                enumConstructorCallTransformer = previous

                return result
            }

            override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val callTransformer = enumConstructorCallTransformer ?:
                throw AssertionError("Enum constructor call outside of enum entry initialization or enum class constructor:\n" + irClass.dump())


                return callTransformer.transform(expression)
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                if (expression.symbol.owner.parentAsClass.kind == ClassKind.ENUM_CLASS) {
                    val callTransformer = enumConstructorCallTransformer ?:
                    throw AssertionError("Enum constructor call outside of enum entry initialization or enum class constructor:\n" + irClass.dump())

                    return callTransformer.transform(expression)
                }
                return expression
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val parameter = expression.symbol.owner
                val loweredParameter = loweredEnumConstructorParameters[parameter]
                return if (loweredParameter == null) {
                    expression
                } else {
                    IrGetValueImpl(expression.startOffset, expression.endOffset, loweredParameter.type,
                            loweredParameter.symbol, expression.origin)
                }
            }
        }
    }
}

private val IrValueParameter.loweredIndex: Int get() = index + 2

private class ParameterMapper(superConstructor: IrConstructor,
                              val constructor: IrConstructor,
                              val useLoweredIndex: Boolean) : IrElementTransformerVoid() {
    private val valueParameters = superConstructor.valueParameters.toSet()

    override fun visitGetValue(expression: IrGetValue): IrExpression {

        val superParameter = expression.symbol.owner as? IrValueParameter ?: return expression
        if (valueParameters.contains(superParameter)) {
            val index = if (useLoweredIndex) superParameter.loweredIndex else superParameter.index
            val parameter = constructor.valueParameters[index]
            return IrGetValueImpl(
                    expression.startOffset, expression.endOffset,
                    parameter.type,
                    parameter.symbol)
        }
        return expression
    }
}
