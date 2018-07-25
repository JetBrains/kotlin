/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import java.util.*

class EnumUsageLowering(val context: JsIrBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetEnumValue(expression: IrGetEnumValue) =
                JsIrBuilder.buildCall(context.enumEntryToGetInstanceFunction[expression.symbol]!!)
        })
    }
}

class EnumClassLowering(val context: JsIrBackendContext) : DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat { declaration ->
            if (declaration is IrClass && declaration.isEnumClass)
                EnumClassTransformer(context, declaration).transform()
            else
                listOf(declaration)
        }
    }
}

class EnumClassTransformer(val context: JsIrBackendContext, private val irClass: IrClass) {
    private val builder = context.createIrBuilder(irClass.symbol)
    private val enumEntries = irClass.declarations.filterIsInstance<IrEnumEntry>()
    private val loweredEnumConstructors = HashMap<IrConstructorSymbol, IrConstructor>()
    private val enumName = irClass.name.identifier

    fun transform(): List<IrDeclaration> {
        // Add `name` and `ordinal` parameters to enum class constructors
        lowerEnumConstructorsSignature()

        // Pass these parameters to delegating constructor calls
        lowerEnumConstructorsBody()

        // The first step creates a new `IrConstructor` with new `IrValueParameter`s so references to old `IrValueParameter`s must be replaced with new ones.
        fixReferencesToConstructorParameters()

        // Create instance variable for each enum entry initialized with `null`
        val entryInstances = createEnumEntryInstanceVariables()

        // Lower `IrEnumConstructorCall`s inside of enum entry class constructors to corresponding `IrDelegatingConstructorCall`s.
        // Add `name` and `ordinal` parameters.
        lowerEnumEntryClassConstructors(entryInstances)

        // Lower `IrEnumConstructorCall`s to corresponding `IrCall`s.
        // Add `name` and `ordinal` constant parameters only for calls to the "enum class" constructors ("enum entry class" constructors
        // already delegate these parameters)
        lowerEnumEntryInitializerExpression()

        // Create boolean flag that indicates if entry instances were initialized.
        val entryInstancesInitializedVar = createEntryInstancesInitializedVar()

        // Create function that initializes all enum entry instances using `IrEnumEntry.initializationExpression`.
        // It should be called on the first `IrGetEnumValue`, consecutive calls to this function will do nothing.
        val initEntryInstancesFun = createInitEntryInstancesFun(entryInstancesInitializedVar, entryInstances)

        // Create entry instance getters. These are used to lower `IrGetEnumValue`.
        val entryGetInstanceFuns = createGetEntryInstanceFuns(initEntryInstancesFun, entryInstances)

        // Create body for `values` and `valueOf` functions
        lowerSyntheticFunctions()

        // Remove IrEnumEntry nodes from class declarations. Replace them with corresponding class declarations (if they have them).
        replaceIrEntriesWithCorrespondingClasses()

        return listOf(irClass) + entryInstances + listOf(entryInstancesInitializedVar, initEntryInstancesFun) + entryGetInstanceFuns
    }

    private fun fixReferencesToConstructorParameters() {
        val fromOldToNewParameter = mutableMapOf<IrValueParameterSymbol, IrValueParameter>()

        loweredEnumConstructors.forEach { (oldCtorSymbol, newCtor) ->
            val oldParameters = oldCtorSymbol.owner.valueParameters
            val newParameters = newCtor.valueParameters

            oldParameters.forEach { old ->
                fromOldToNewParameter[old.symbol] = newParameters.single { it.name == old.name }
            }
        }

        irClass.transformChildrenVoid(object: IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                fromOldToNewParameter[expression.symbol]?.let {
                    return builder.irGet(it)
                }

                return super.visitGetValue(expression)
            }
        })
    }

    private fun createEnumValueOfBody(): IrBody {
        val valueOfFun = findFunctionDescriptorForMemberWithSyntheticBodyKind(IrSyntheticBodyKind.ENUM_VALUEOF)
        val nameParameter = valueOfFun.valueParameters[0]
        val entryInstanceToFunction = context.enumEntryToGetInstanceFunction

        return context.createIrBuilder(valueOfFun.symbol).run {
            irBlockBody {
                +irReturn(
                    irWhen(
                        irClass.defaultType,
                        enumEntries.map {
                            val getInstance = entryInstanceToFunction[it.symbol]!!
                            irBranch(
                                irEquals(irString(it.name.identifier), irGet(nameParameter)), irCall(getInstance)
                            )
                        } + irElseBranch(irCall(context.irBuiltIns.throwIseSymbol))
                    )
                )
            }
        }
    }

    private fun lowerEnumConstructorsSignature() {
        irClass.declarations.transform { declaration ->
            if (declaration is IrConstructor) {
                transformEnumConstructor(declaration, irClass)
            } else
                declaration
        }
    }

    private fun lowerEnumConstructorsBody() {
        irClass.declarations.filterIsInstance<IrConstructor>().forEach {
            IrEnumClassConstructorTransformer(it).transformBody()
        }
    }

    private fun lowerEnumEntryClassConstructors(entryInstances: List<IrVariable>) {
        for ((entry, instance) in enumEntries.zip(entryInstances)) {
            entry.correspondingClass?.constructors?.forEach {
                it.transformChildrenVoid(IrEnumEntryClassConstructorTransformer(entry, true))

                // Initialize entry instance at the beginning of constructor so it can be used inside constructor body
                (it.body as? IrBlockBody)?.apply {
                    statements.add(0, context.createIrBuilder(it.symbol).run {
                        irSetVar(instance.symbol, irGet(entry.correspondingClass!!.thisReceiver!!))
                    })
                }
            }
        }
    }

    private fun lowerEnumEntryInitializerExpression() {
        for (entry in enumEntries) {
            entry.initializerExpression =
                    entry.initializerExpression?.transform(IrEnumEntryClassConstructorTransformer(entry, false), null)
        }
    }

    private fun createEnumEntryInstanceVariables() = enumEntries.map { enumEntry ->
        val type = enumEntry.getType().makeNullable()
        val name = "${enumName}_${enumEntry.name.identifier}_instance"
        builder.run {
            scope.createTemporaryVariable(irImplicitCast(irNull(), type), name)
        }
    }

    private fun replaceIrEntriesWithCorrespondingClasses() {
        irClass.declarations.transformFlat {
            listOfNotNull(if (it is IrEnumEntry) it.correspondingClass else it)
        }
    }

    private fun lowerSyntheticFunctions() {
        irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitSyntheticBody(body: IrSyntheticBody): IrBody {
                return when (body.kind) {
                    IrSyntheticBodyKind.ENUM_VALUES -> builder.irBlockBody { } // TODO: Implement
                    IrSyntheticBodyKind.ENUM_VALUEOF -> createEnumValueOfBody()
                }
            }
        })
    }

    private fun createGetEntryInstanceFuns(
        initEntryInstancesFun: IrSimpleFunction,
        entryInstances: List<IrVariable>
    ) = enumEntries.mapIndexed { index, enumEntry ->
        buildFunction(
            name = "${enumName}_${enumEntry.name.identifier}_getInstance",
            returnType = enumEntry.getType()
        ) {
            +irCall(initEntryInstancesFun)
            +irReturn(irGet(entryInstances[index]))
        }.apply {
            context.enumEntryToGetInstanceFunction[enumEntry.symbol] = this@apply.symbol
        }
    }

    private fun createInitEntryInstancesFun(
        entryInstancesInitializedVar: IrVariable,
        entryInstances: List<IrVariable>
    ) = buildFunction("${enumName}_initEntries") {
        +irIfThen(irGet(entryInstancesInitializedVar), irReturnUnit())
        +irSetVar(entryInstancesInitializedVar.symbol, irBoolean(true))
        for ((entry, instanceVar) in enumEntries.zip(entryInstances)) {
            +irSetVar(instanceVar.symbol, entry.initializerExpression!!)
        }
    }

    private fun createEntryInstancesInitializedVar(): IrVariable {
        return builder.scope.createTemporaryVariable(
            builder.irBoolean(false),
            "${enumName}_entriesInitialized"
        )
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
            var constructor = expression.symbol.owner
            val constructorWasTransformed = constructor.symbol in loweredEnumConstructors

            if (constructorWasTransformed)
                constructor = loweredEnumConstructors[constructor.symbol]!!

            return builder.irDelegatingConstructorCall(constructor).apply {
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

    private inner class IrEnumEntryClassConstructorTransformer(val entry: IrEnumEntry, val isInsideConstructor: Boolean) :
        IrElementTransformerVoid() {

        private fun buildConstructorCall(constructor: IrConstructor) =
            if (isInsideConstructor)
                builder.irDelegatingConstructorCall(constructor)
            else
                builder.irCall(constructor)

        override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
            var constructor = expression.symbol.owner
            val constructorWasTransformed = constructor.symbol in loweredEnumConstructors

            // Enum entry class constructors are not transformed
            if (constructorWasTransformed)
                constructor = loweredEnumConstructors[constructor.symbol]!!

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
            enumConstructor.isInline,
            enumConstructor.isExternal,
            enumConstructor.isPrimary
        ).apply {
            loweredConstructorDescriptor.bind(this)
            parent = enumClass
            returnType = enumConstructor.returnType
            valueParameters += JsIrBuilder.buildValueParameter("name", 0, context.irBuiltIns.stringType).also { it.parent = this }
            valueParameters += JsIrBuilder.buildValueParameter("ordinal", 1, context.irBuiltIns.intType).also { it.parent = this }
            copyParameterDeclarationsFrom(enumConstructor)
            body = enumConstructor.body
            loweredEnumConstructors[enumConstructor.symbol] = this
        }
    }

    private fun findFunctionDescriptorForMemberWithSyntheticBodyKind(kind: IrSyntheticBodyKind): IrFunction =
        irClass.declarations.asSequence().filterIsInstance<IrFunction>()
            .first {
                it.body.let { body ->
                    body is IrSyntheticBody && body.kind == kind
                }
            }

    private fun buildFunction(
        name: String,
        returnType: IrType = context.irBuiltIns.unitType,
        bodyBuilder: IrBlockBodyBuilder.() -> Unit
    ) = JsIrBuilder.buildFunction(name).also {
        it.returnType = returnType
        it.parent = irClass
        it.body = context.createIrBuilder(it.symbol).irBlockBody(it, bodyBuilder)
    }

    private fun IrEnumEntry.getNameExpression() = builder.irString(this.name.identifier)
    private fun IrEnumEntry.getOrdinalExpression() = builder.irInt(enumEntries.indexOf(this))
    private fun IrEnumEntry.getType() = (correspondingClass ?: irClass).defaultType
}
