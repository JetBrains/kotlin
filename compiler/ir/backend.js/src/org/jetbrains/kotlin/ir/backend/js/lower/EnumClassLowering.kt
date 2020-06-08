/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.getOrPut
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.ir.isExpect
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.toJsArrayLiteral
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedFieldDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class EnumUsageLowering(val context: JsIrBackendContext) : BodyLoweringPass {
    private var IrEnumEntry.getInstanceFun by context.mapping.enumEntryToGetInstanceFun

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetEnumValue(expression: IrGetEnumValue): IrExpression {
                val enumEntry = expression.symbol.owner
                val klass = enumEntry.parent as IrClass
                return if (klass.isExternal) lowerExternalEnumEntry(enumEntry, klass) else lowerEnumEntry(enumEntry, klass)
            }
        })
    }

    private fun lowerExternalEnumEntry(enumEntry: IrEnumEntry, klass: IrClass) =
        context.mapping.enumEntryToInstanceField.getOrPut(enumEntry) { createFieldForEntry(enumEntry, klass) }.let {
            JsIrBuilder.buildGetField(it.symbol, classAsReceiver(klass), null, klass.defaultType)
        }

    private fun classAsReceiver(irClass: IrClass): IrExpression {
        val intrinsic = context.intrinsics.jsClass
        return JsIrBuilder.buildCall(intrinsic, context.irBuiltIns.anyType, listOf(irClass.defaultType))
    }

    private fun createFieldForEntry(entry: IrEnumEntry, irClass: IrClass): IrField {
        val descriptor = WrappedFieldDescriptor()
        val symbol = IrFieldSymbolImpl(descriptor)
        return entry.run {
            IrFieldImpl(
                startOffset, endOffset, origin, symbol, name, irClass.defaultType, Visibilities.PUBLIC,
                isFinal = false, isExternal = true, isStatic = true,
                isFakeOverride = entry.isFakeOverride
            ).also {
                descriptor.bind(it)
                it.parent = irClass

                // TODO need a way to emerge local declarations from BodyLoweringPass
                stageController.unrestrictDeclarationListsAccess {
                    irClass.declarations += it
                }
            }
        }
    }

    private fun lowerEnumEntry(enumEntry: IrEnumEntry, klass: IrClass) =
        enumEntry.getInstanceFun!!.run { JsIrBuilder.buildCall(symbol) }
}


private fun createEntryAccessorName(enumName: String, enumEntry: IrEnumEntry) =
    "${enumName}_${enumEntry.name.identifier}_getInstance"

private fun IrEnumEntry.getType(irClass: IrClass) = (correspondingClass ?: irClass).defaultType

// Should be applied recursively
class EnumClassConstructorLowering(val context: JsCommonBackendContext) : DeclarationTransformer {

    private var IrConstructor.newConstructor by context.mapping.enumConstructorToNewConstructor
    private var IrClass.correspondingEntry by context.mapping.enumClassToCorrespondingEnumEntry
    private var IrValueDeclaration.valueParameter by context.mapping.enumConstructorOldToNewValueParameters

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        (declaration.parent as? IrClass)?.let { irClass ->
            if (!irClass.isEnumClass || irClass.isExpect || irClass.isEffectivelyExternal()) return null

            if (declaration is IrConstructor) {
                // Add `name` and `ordinal` parameters to enum class constructors
                return listOf(transformEnumConstructor(declaration, irClass))
            }

            if (declaration is IrEnumEntry) {
                declaration.correspondingClass?.let { klass ->
                    klass.correspondingEntry = declaration
                }
            }
        }

        return null
    }

    private fun transformEnumConstructor(enumConstructor: IrConstructor, enumClass: IrClass): IrConstructor {
        val loweredConstructorDescriptor = WrappedClassConstructorDescriptor()
        val loweredConstructorSymbol = IrConstructorSymbolImpl(loweredConstructorDescriptor)

        return IrConstructorImpl(
            enumConstructor.startOffset,
            enumConstructor.endOffset,
            enumConstructor.origin,
            loweredConstructorSymbol,
            enumConstructor.name,
            enumConstructor.visibility,
            enumConstructor.returnType,
            isInline = enumConstructor.isInline,
            isExternal = enumConstructor.isExternal,
            isPrimary = enumConstructor.isPrimary,
            isExpect = enumConstructor.isExpect
        ).apply {
            loweredConstructorDescriptor.bind(this)
            parent = enumClass
            valueParameters += JsIrBuilder.buildValueParameter("name", 0, context.irBuiltIns.stringType).also { it.parent = this }
            valueParameters += JsIrBuilder.buildValueParameter("ordinal", 1, context.irBuiltIns.intType).also { it.parent = this }
            copyParameterDeclarationsFrom(enumConstructor)

            val newConstructor = this
            enumConstructor.newConstructor = this

            enumConstructor.body?.let { oldBody ->
                body = IrBlockBodyImpl(oldBody.startOffset, oldBody.endOffset) {
                    statements += (oldBody as IrBlockBody).statements

                    context.fixReferencesToConstructorParameters(enumClass, this)

                    acceptVoid(PatchDeclarationParentsVisitor(enumClass))

                    body = this
                }
            }

            // TODO except for `fixReferencesToConstructorParameters` this code seems to be obsolete
            val oldParameters = enumConstructor.valueParameters
            val newParameters = valueParameters
            oldParameters.forEach { old ->
                // TODO Match by index?
                val new = newParameters.single { it.name == old.name }
                old.valueParameter = new

                old.defaultValue?.let { default ->
                    new.defaultValue = IrExpressionBodyImpl(default.startOffset, default.endOffset) {
                        expression = default.expression
                        expression.patchDeclarationParents(newConstructor)
                        context.fixReferencesToConstructorParameters(enumClass, this)
                    }
                }
            }
        }
    }
}

// The first step creates a new `IrConstructor` with new `IrValueParameter`s so references to old `IrValueParameter`s must be replaced with new ones.
private fun JsCommonBackendContext.fixReferencesToConstructorParameters(irClass: IrClass, body: IrBody) {
    body.transformChildrenVoid(object : IrElementTransformerVoid() {
        private val builder = createIrBuilder(irClass.symbol)

        override fun visitGetValue(expression: IrGetValue): IrExpression {
            mapping.enumConstructorOldToNewValueParameters[expression.symbol.owner]?.let {
                return builder.irGet(it)
            }

            return super.visitGetValue(expression)
        }
    })
}

class EnumClassConstructorBodyTransformer(val context: JsCommonBackendContext) : BodyLoweringPass {

    private var IrConstructor.newConstructor by context.mapping.enumConstructorToNewConstructor
    private var IrClass.correspondingEntry by context.mapping.enumClassToCorrespondingEnumEntry

    override fun lower(irBody: IrBody, container: IrDeclaration) {

        (container.parent as? IrClass)?.let { irClass ->

            // TODO Don't apply to everything
            context.fixReferencesToConstructorParameters(irClass, irBody)

            if (container is IrConstructor) {

                if (irClass.goodEnum) {
                    // Pass new parameters to delegating constructor calls
                    lowerEnumConstructorsBody(container)
                }

                irClass.correspondingEntry?.let { enumEntry ->
                    // Lower `IrEnumConstructorCall`s inside of enum entry class constructors to corresponding `IrDelegatingConstructorCall`s.
                    // Add `name` and `ordinal` parameters.
                    lowerEnumEntryClassConstructors(irClass, enumEntry, container)
                }
            }

            if (container is IrEnumEntry) {
                // Lower `IrEnumConstructorCall`s to corresponding `IrCall`s.
                // Add `name` and `ordinal` constant parameters only for calls to the "enum class" constructors ("enum entry class" constructors
                // already delegate these parameters)
                lowerEnumEntryInitializerExpression(irClass, container)
            }
        }
    }

    private fun lowerEnumConstructorsBody(constructor: IrConstructor) {
        IrEnumClassConstructorTransformer(constructor).transformBody()
    }

    private inner class IrEnumClassConstructorTransformer(val constructor: IrConstructor) : IrElementTransformerVoid() {
        val builder = context.createIrBuilder(constructor.symbol)

        fun transformBody() {
            constructor.body?.transformChildrenVoid(this)
        }

        override fun visitEnumConstructorCall(expression: IrEnumConstructorCall) =
            builder.irDelegatingConstructorCall(expression.symbol.owner).apply {
                for (i in 0..1) {
                    putValueArgument(i, builder.irGet(constructor.valueParameters[i]))
                }
            }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
            val delegatingConstructor = expression.symbol.owner. let { it.newConstructor ?: it }

            return builder.irDelegatingConstructorCall(delegatingConstructor).apply {
                var valueArgIdx = 0
                for (i in 0..1) {
                    putValueArgument(valueArgIdx++, builder.irGet(constructor.valueParameters[i]))
                }
                for (i in 0 until expression.valueArgumentsCount) {
                    putValueArgument(valueArgIdx++, expression.getValueArgument(i))
                }
            }
        }
    }


    private fun lowerEnumEntryClassConstructors(irClass: IrClass, entry: IrEnumEntry, constructor: IrConstructor) {
        constructor.transformChildrenVoid(IrEnumEntryClassConstructorTransformer(irClass, entry, true))
    }

    private inner class IrEnumEntryClassConstructorTransformer(
        val irClass: IrClass,
        val entry: IrEnumEntry,
        val isInsideConstructor: Boolean
    ) :
        IrElementTransformerVoid() {

        private val enumEntries = irClass.enumEntries

        private val builder = context.createIrBuilder(irClass.symbol)

        private fun IrEnumEntry.getNameExpression() = builder.irString(this.name.identifier)
        private fun IrEnumEntry.getOrdinalExpression() = builder.irInt(enumEntries.indexOf(this))

        private fun buildConstructorCall(constructor: IrConstructor) =
            if (isInsideConstructor)
                builder.irDelegatingConstructorCall(constructor)
            else
                builder.irCall(constructor)

        override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
            var constructor = expression.symbol.owner
            val constructorWasTransformed = constructor.newConstructor != null

            // Enum entry class constructors are not transformed
            if (constructorWasTransformed)
                constructor = constructor.newConstructor!!

            return buildConstructorCall(constructor).apply {
                var valueArgIdx = 0

                // Enum entry class constructors already delegate name and ordinal parameters in their body
                if (constructorWasTransformed) {
                    putValueArgument(valueArgIdx++, entry.getNameExpression())
                    putValueArgument(valueArgIdx++, entry.getOrdinalExpression())
                }
                for (i in 0 until expression.valueArgumentsCount) {
                    putValueArgument(valueArgIdx++, expression.getValueArgument(i))
                }
            }
        }
    }

    private fun lowerEnumEntryInitializerExpression(irClass: IrClass, entry: IrEnumEntry) {
        entry.initializerExpression =
            entry.initializerExpression?.transform(IrEnumEntryClassConstructorTransformer(irClass, entry, false), null)
    }
}

//-------------------------------------------------------

private val IrClass.goodEnum: Boolean
    get() = isEnumClass && !isExpect && !isEffectivelyExternal()

class EnumEntryInstancesLowering(val context: JsIrBackendContext) : DeclarationTransformer {

    private var IrEnumEntry.correspondingField by context.mapping.enumEntryToCorrespondingField

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrEnumEntry) {
            val irClass = declaration.parentAsClass
            if (irClass.goodEnum) {
                // Create instance variable for each enum entry initialized with `null`
                return listOf(declaration, createEnumEntryInstanceVariable(irClass, declaration))
            }
        }
        return null
    }

    private fun createEnumEntryInstanceVariable(irClass: IrClass, enumEntry: IrEnumEntry): IrField {
        val enumName = irClass.name.identifier

        val result = buildField {
            name = Name.identifier("${enumName}_${enumEntry.name.identifier}_instance")
            type = enumEntry.getType(irClass).makeNullable()
            isStatic = true
        }.apply {
            parent = irClass
            initializer = null
        }

        enumEntry.correspondingField = result

        return result
    }
}

class EnumEntryInstancesBodyLowering(val context: JsIrBackendContext) : BodyLoweringPass {

    private var IrEnumEntry.correspondingField by context.mapping.enumEntryToCorrespondingField

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container is IrConstructor && container.constructedClass.kind == ClassKind.ENUM_ENTRY) {
            val entryClass = container.constructedClass
            val enum = entryClass.parentAsClass
            if (enum.goodEnum) {
                val entry = enum.declarations.filterIsInstance<IrEnumEntry>().find { it.correspondingClass === entryClass }!!

                //In ES6 using `this` before superCall is unavailable, so
                //need to find superCall and put `instance = this` after it
                val index = (irBody as IrBlockBody).statements
                    .indexOfFirst { it is IrTypeOperatorCall && it.argument is IrDelegatingConstructorCall } + 1

                (irBody as IrBlockBody).statements.add(index, context.createIrBuilder(container.symbol).run {
                    irSetField(null, entry.correspondingField!!, irGet(entryClass.thisReceiver!!))
                })
            }
        }
    }
}

class EnumClassCreateInitializerLowering(val context: JsIrBackendContext) : DeclarationTransformer {

    private var IrEnumEntry.correspondingField by context.mapping.enumEntryToCorrespondingField
    private var IrClass.initEntryInstancesFun: IrSimpleFunction? by context.mapping.enumClassToInitEntryInstancesFun

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrClass && declaration.goodEnum) {
            // Create boolean flag that indicates if entry instances were initialized.
            val entryInstancesInitializedVar = createEntryInstancesInitializedVar(declaration)

            // Create function that initializes all enum entry instances using `IrEnumEntry.initializationExpression`.
            // It should be called on the first `IrGetEnumValue`, consecutive calls to this function will do nothing.
            val initEntryInstancesFun = createInitEntryInstancesFun(declaration, entryInstancesInitializedVar)

            declaration.initEntryInstancesFun = initEntryInstancesFun

            // TODO Why not move to upper level?
            // TODO Also doesn't fit the transformFlat-ish API
            stageController.unrestrictDeclarationListsAccess {
                declaration.declarations += entryInstancesInitializedVar
                declaration.declarations += initEntryInstancesFun
            }

            return null
        }

        return null
    }

    private fun createEntryInstancesInitializedVar(irClass: IrClass): IrField = buildField {
        val enumName = irClass.name.identifier
        name = Name.identifier("${enumName}_entriesInitialized")
        type = context.irBuiltIns.booleanType
        isStatic = true
    }.apply {
        parent = irClass
        initializer = null
    }

    private fun createInitEntryInstancesFun(irClass: IrClass, entryInstancesInitializedField: IrField): IrSimpleFunction =
        buildFunction(irClass, "${irClass.name.identifier}_initEntries", context.irBuiltIns.unitType).also {
            it.body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                statements += context.createIrBuilder(it.symbol).irBlockBody(it) {
                    +irIfThen(irGetField(null, entryInstancesInitializedField), irReturnUnit())
                    +irSetField(null, entryInstancesInitializedField, irBoolean(true))

                    irClass.enumEntries.forEach { entry ->
                        entry.correspondingField?.let { instanceField ->
                            +irSetField(null, instanceField, entry.initializerExpression!!.expression)
                        }
                    }
                }.also {
                    // entry.initializerExpression can have local declarations
                    it.acceptVoid(PatchDeclarationParentsVisitor(irClass))
                }.statements
            }
        }
}

private fun buildFunction(
    irClass: IrClass,
    name: String,
    returnType: IrType
) = JsIrBuilder.buildFunction(name, returnType, irClass)

class EnumEntryCreateGetInstancesFunsLowering(val context: JsIrBackendContext): DeclarationTransformer {

    private var IrEnumEntry.correspondingField by context.mapping.enumEntryToCorrespondingField
    private var IrClass.initEntryInstancesFun: IrSimpleFunction? by context.mapping.enumClassToInitEntryInstancesFun

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrEnumEntry) {
            val irClass = declaration.parentAsClass
            if (irClass.goodEnum) {
                // Create entry instance getters. These are used to lower `IrGetEnumValue`.
                val entryGetInstanceFun = createGetEntryInstanceFun(irClass, declaration, irClass.initEntryInstancesFun!!)

                // TODO prettify
                entryGetInstanceFun.parent = irClass.parent
                stageController.unrestrictDeclarationListsAccess {
                    (irClass.parent as IrDeclarationContainer).declarations += entryGetInstanceFun
                }

                return listOf(declaration) // TODO not null?
            }
        }

        return null
    }

    private fun createGetEntryInstanceFun(irClass: IrClass, enumEntry: IrEnumEntry, initEntryInstancesFun: IrSimpleFunction): IrSimpleFunction {

        return context.mapping.enumEntryToGetInstanceFun.getOrPut(enumEntry) { buildFunction(irClass, createEntryAccessorName(irClass.name.identifier, enumEntry), enumEntry.getType(irClass)) }
            .also {
                it.body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                    statements += context.createIrBuilder(it.symbol).irBlockBody(it) {
                        +irCall(initEntryInstancesFun)
                        +irReturn(irGetField(null, enumEntry.correspondingField!!))
                    }.statements
                }
            }
    }
}

class EnumSyntheticFunctionsLowering(val context: JsIrBackendContext): DeclarationTransformer {

    private var IrEnumEntry.getInstanceFun by context.mapping.enumEntryToGetInstanceFun

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction) {
            (declaration.body as? IrSyntheticBody)?.let { body ->
                val kind = body.kind

                declaration.parents.filterIsInstance<IrClass>().firstOrNull { it.goodEnum }?.let { irClass ->
                    declaration.body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                        statements += when (kind) {
                            IrSyntheticBodyKind.ENUM_VALUES -> createEnumValuesBody(declaration, irClass)
                            IrSyntheticBodyKind.ENUM_VALUEOF -> createEnumValueOfBody(declaration, irClass)
                        }.statements
                    }
                }
            }
        }

        return null
    }

    private val throwISESymbol = context.throwISEsymbol

    private fun createEnumValueOfBody(valueOfFun: IrFunction, irClass: IrClass): IrBlockBody {
        val nameParameter = valueOfFun.valueParameters[0]

        return context.createIrBuilder(valueOfFun.symbol).run {
            irBlockBody {
                +irReturn(
                    irWhen(
                        irClass.defaultType,
                        irClass.enumEntries.map {
                            irBranch(
                                irEquals(irString(it.name.identifier), irGet(nameParameter)), irCall(it.getInstanceFun!!)
                            )
                        } + irElseBranch(irCall(throwISESymbol))
                    )
                )
            }
        }
    }

    private fun List<IrExpression>.toArrayLiteral(arrayType: IrType, elementType: IrType): IrExpression {
        val irVararg = IrVarargImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, arrayType, elementType, this)

        return IrCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, arrayType,
            context.intrinsics.arrayLiteral,
            typeArgumentsCount = 0,
            valueArgumentsCount = 1
        ).apply {
            putValueArgument(0, irVararg)
        }
    }

    private fun createEnumValuesBody(valuesFun: IrFunction, irClass: IrClass): IrBlockBody {
        val backendContext = context
        return context.createIrBuilder(valuesFun.symbol).run {
            irBlockBody {
                +irReturn(
                    irClass.enumEntries.map { irCall(it.getInstanceFun!!) }
                        .toJsArrayLiteral(backendContext, valuesFun.returnType, irClass.defaultType)
                )
            }
        }
    }
}

private val IrClass.enumEntries: List<IrEnumEntry>
    get() = declarations.filterIsInstance<IrEnumEntry>()

// Should be applied recursively
class EnumClassRemoveEntriesLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        // Remove IrEnumEntry nodes from class declarations. Replace them with corresponding class declarations (if they have them).
        if (declaration is IrEnumEntry && !declaration.isExpect && !declaration.isEffectivelyExternal()) {
            return listOfNotNull(declaration.correspondingClass)
        }

        return null
    }
}