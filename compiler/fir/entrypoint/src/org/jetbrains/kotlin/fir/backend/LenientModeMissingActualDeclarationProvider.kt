/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.actualizer.IrMissingActualDeclarationProvider
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.removeAnnotations
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.runIf

@OptIn(UnsafeDuringIrConstructionAPI::class)
class LenientModeMissingActualDeclarationProvider(
    private val builtins: Fir2IrBuiltinSymbolsContainer,
    private val classifierStorage: Fir2IrClassifierStorage,
    private val symbolProvider: FirSymbolProvider,
) : IrMissingActualDeclarationProvider() {
    companion object {
        fun initializeIfNeeded(c: Fir2IrComponents): LenientModeMissingActualDeclarationProvider? {
            return runIf(c.session.languageVersionSettings.getFlag(AnalysisFlags.lenientMode)) {
                LenientModeMissingActualDeclarationProvider(c.builtins, c.classifierStorage, c.session.symbolProvider)
            }
        }
    }

    private val notImplementedErrorConstructorSymbol by lazy(LazyThreadSafetyMode.NONE) {
        val firSymbol = symbolProvider.getClassLikeSymbolByClassId(ClassId.fromString("kotlin/NotImplementedError")) as FirClassSymbol
        classifierStorage.getIrClassSymbol(firSymbol).constructors.first()
    }

    override fun provideSymbolForMissingActual(
        expectSymbol: IrSymbol,
        containingExpectClassSymbol: IrClassSymbol?,
        containingActualClassSymbol: IrClassSymbol?,
    ): IrSymbol? {
        if (containingExpectClassSymbol != null || containingActualClassSymbol != null) {
            return null
        }

        val declaration = expectSymbol.owner as? IrDeclaration ?: return null
        return buildStub(expectSymbol, declaration.parent)
    }

    private fun buildStub(
        expectSymbol: IrSymbol,
        parent: IrDeclarationParent,
    ): IrSymbol? {
        return when (expectSymbol) {
            is IrClassSymbol -> buildClassStub(expectSymbol, parent)
            is IrPropertySymbol -> buildPropertyStub(expectSymbol, parent)
            is IrSimpleFunctionSymbol -> buildFunctionStub(expectSymbol, parent)
            is IrConstructorSymbol -> buildConstructorStub(expectSymbol, parent)
            else -> null
        }
    }

    private fun buildClassStub(expectSymbol: IrClassSymbol, parent: IrDeclarationParent): IrClassSymbol {
        val symbol = IrClassSymbolImpl()
        val owner = expectSymbol.owner

        val clazz = IrFactoryImpl.createClass(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.STUB_FOR_LENIENT,
            symbol = symbol,
            name = owner.name,
            visibility = owner.visibility,
            kind = owner.kind,
            modality = owner.modality,
            isCompanion = owner.isCompanion,
            isInner = owner.isInner,
            isData = owner.isData,
            isValue = owner.isValue,
            isFun = owner.isFun,
            isExpect = false,
        )
        clazz.thisReceiver = owner.thisReceiver?.copyAsStub(clazz)
        clazz.annotations = owner.annotations
        clazz.parent = parent
        clazz.declarations.addAll(owner.declarations.mapNotNull { buildStub(it.symbol, clazz)?.owner as IrDeclaration? })

        return symbol
    }

    private fun buildPropertyStub(expectSymbol: IrPropertySymbol, parent: IrDeclarationParent): IrPropertySymbol {
        val symbol = IrPropertySymbolImpl()
        val owner = expectSymbol.owner

        val property = IrFactoryImpl.createProperty(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.STUB_FOR_LENIENT,
            symbol = symbol,
            name = owner.name,
            visibility = owner.visibility,
            modality = owner.modality,
            isVar = owner.isVar,
            isConst = false,
            isLateinit = false,
            isDelegated = false,
            isExpect = false
        )

        property.getter = owner.getter?.let {
            val function = buildFunctionStub(it.symbol, parent).owner
            function.correspondingPropertySymbol = symbol
            function
        }
        property.setter = owner.setter?.let {
            val function = buildFunctionStub(it.symbol, parent).owner
            function.correspondingPropertySymbol = symbol
            function
        }
        property.annotations = owner.annotations
        property.parent = parent

        return symbol
    }

    private fun buildConstructorStub(expectSymbol: IrConstructorSymbol, parent: IrDeclarationParent): IrConstructorSymbol {
        val symbol = IrConstructorSymbolImpl()
        val owner = expectSymbol.owner

        val constructor = IrFactoryImpl.createConstructor(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.STUB_FOR_LENIENT,
            symbol = symbol,
            name = owner.name,
            visibility = owner.visibility,
            isInline = owner.isInline,
            returnType = owner.returnType,
            isPrimary = false,
            isExpect = false,
        )

        fillFunction(constructor, owner, parent)
        // TODO handle or forbid cases where class doesn't extend Any
        constructor.body = IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET).apply {
            statements += IrDelegatingConstructorCallImplWithShape(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = builtins.unitType,
                symbol = builtins.anyClass.constructors.first(),
                typeArgumentsCount = 0,
                valueArgumentsCount = 0,
                contextParameterCount = 0,
                hasDispatchReceiver = false,
                hasExtensionReceiver = false,
            )
            statements += IrInstanceInitializerCallImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                classSymbol = (parent as IrClass).symbol,
                type = builtins.unitType
            )
        }

        return symbol
    }

    private fun buildFunctionStub(expectSymbol: IrSimpleFunctionSymbol, parent: IrDeclarationParent): IrSimpleFunctionSymbol {
        val symbol = IrSimpleFunctionSymbolImpl()
        val owner = expectSymbol.owner

        val function = IrFactoryImpl.createSimpleFunction(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.STUB_FOR_LENIENT,
            symbol = symbol,
            name = owner.name,
            visibility = owner.visibility,
            isInline = owner.isInline,
            returnType = owner.returnType,
            modality = owner.modality,
            isTailrec = owner.isTailrec,
            isSuspend = owner.isSuspend,
            isOperator = owner.isOperator,
            isInfix = owner.isInfix,
            isExpect = false,
        )

        fillFunction(function, owner, parent)
        function.body = IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET).apply {
            val returnType = owner.returnType
            if (returnType == builtins.unitType) {
                return@apply
            }
            if (returnType.isNullable() || returnType.isPrimitiveType() || returnType.isString()) {
                statements += IrReturnImpl(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    type = builtins.nothingType,
                    returnTargetSymbol = symbol,
                    value = generateDefaultReturnValue(returnType)
                )
            } else {
                statements += IrThrowImpl(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    type = builtins.nothingType,
                    value = IrConstructorCallImpl(
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        type = notImplementedErrorConstructorSymbol.owner.returnType,
                        symbol = notImplementedErrorConstructorSymbol,
                        typeArgumentsCount = 0,
                        constructorTypeArgumentsCount = 0,
                    )
                )
            }
        }

        return symbol
    }

    private fun generateDefaultReturnValue(returnType: IrType): IrConstImpl {
        var kind: IrConstKind
        var value: Any?

        when (returnType) {
            builtins.stringType -> {
                kind = IrConstKind.String
                value = ""
            }
            builtins.booleanType -> {
                kind = IrConstKind.Boolean
                value = false
            }
            builtins.byteType -> {
                kind = IrConstKind.Byte
                value = 0.toByte()
            }
            builtins.shortType -> {
                kind = IrConstKind.Short
                value = 0.toShort()
            }
            builtins.intType -> {
                kind = IrConstKind.Int
                value = 0
            }
            builtins.longType -> {
                kind = IrConstKind.Long
                value = 0L
            }
            builtins.charType -> {
                kind = IrConstKind.Char
                value = Char.MIN_VALUE
            }
            builtins.floatType -> {
                kind = IrConstKind.Float
                value = 0.0f
            }
            builtins.doubleType -> {
                kind = IrConstKind.Double
                value = 0.0
            }
            else -> {
                require(returnType.isNullable()) { "Cannot generate default return value for ${returnType.render()}" }
                kind = IrConstKind.Null
                value = null
            }
        }

        return IrConstImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = returnType.removeAnnotations(),
            kind = kind,
            value = value
        )
    }

    private fun fillFunction(function: IrFunction, owner: IrFunction, parent: IrDeclarationParent) {
        function.parameters = owner.parameters.map { it.copyAsStub(function) }
        function.typeParameters = owner.typeParameters.map { it.copyAsStub(function) }
        function.annotations = owner.annotations
        function.parent = parent
    }

    private fun IrValueParameter.copyAsStub(parent: IrDeclarationParent): IrValueParameter {
        return IrFactoryImpl.createValueParameter(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.STUB_FOR_LENIENT,
            symbol = IrValueParameterSymbolImpl(),
            kind = kind,
            name = name,
            type = type,
            isAssignable = isAssignable,
            varargElementType = varargElementType,
            isCrossinline = isCrossinline,
            isNoinline = isNoinline,
            isHidden = isHidden
        ).also {
            it.parent = parent
            it.annotations = annotations
        }
    }

    private fun IrTypeParameter.copyAsStub(parent: IrDeclarationParent): IrTypeParameter {
        return IrFactoryImpl.createTypeParameter(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.STUB_FOR_LENIENT,
            symbol = IrTypeParameterSymbolImpl(),
            name = name,
            variance = variance,
            index = index,
            isReified = isReified
        ).also {
            it.parent = parent
            it.annotations = annotations
        }
    }
}