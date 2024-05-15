/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.utils.ConversionTypeOrigin
import org.jetbrains.kotlin.fir.backend.utils.toIrSymbol
import org.jetbrains.kotlin.fir.backend.utils.unsubstitutedScope
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isBoolean
import org.jetbrains.kotlin.fir.types.isInt
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

class Fir2IrBuiltinSymbolsContainer(private val c: Fir2IrComponents) {
    private val session: FirSession
        get() = c.session

    private val symbolProvider: FirSymbolProvider
        get() = session.symbolProvider

    val irFactory: IrFactory = c.irFactory

    val booleanNotSymbol: IrSimpleFunctionSymbol by lazy {
        val firFunction = findFirMemberFunctions(StandardClassIds.Boolean, OperatorNameConventions.NOT)
            .first { it.resolvedReturnType.isBoolean }
        findFunction(firFunction)
    }

    private fun findFirMemberFunctions(classId: ClassId, name: Name): List<FirNamedFunctionSymbol> {
        val klass = symbolProvider.getClassLikeSymbolByClassId(classId) as FirRegularClassSymbol
        val scope = klass.unsubstitutedScope(c)
        return scope.getFunctions(name)
    }

    val anyClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Any) }

    val anyType: IrType get() = anyClass.defaultTypeWithoutArguments
    val anyNType: IrType by lazy { anyType.makeNullable() }

    val numberClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Number) }

    val nothingClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Nothing) }
    val nothingType: IrType get() = nothingClass.defaultTypeWithoutArguments
    val nothingNType: IrType by lazy { nothingType.makeNullable() }

    val unitClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Unit) }
    val unitType: IrType get() = unitClass.defaultTypeWithoutArguments

    val booleanClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Boolean) }
    val booleanType: IrType get() = booleanClass.defaultTypeWithoutArguments

    val charClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Char) }
    val charType: IrType get() = charClass.defaultTypeWithoutArguments

    val byteClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Byte) }
    val byteType: IrType get() = byteClass.defaultTypeWithoutArguments

    val shortClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Short) }
    val shortType: IrType get() = shortClass.defaultTypeWithoutArguments

    val intClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Int) }
    val intType: IrType get() = intClass.defaultTypeWithoutArguments

    val longClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Long) }
    val longType: IrType get() = longClass.defaultTypeWithoutArguments

    val floatClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Float) }
    val floatType: IrType get() = floatClass.defaultTypeWithoutArguments

    val doubleClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Double) }
    val doubleType: IrType get() = doubleClass.defaultTypeWithoutArguments

    val charSequenceClass: IrClassSymbol by lazy { loadClass(StandardClassIds.CharSequence) }

    val stringClass: IrClassSymbol by lazy { loadClass(StandardClassIds.String) }
    val stringType: IrType get() = stringClass.defaultTypeWithoutArguments

    val extensionFunctionTypeAnnotationCall: IrConstructorCall? by lazy {
        val firSymbol =
            session.symbolProvider.getClassLikeSymbolByClassId(StandardClassIds.Annotations.ExtensionFunctionType) as? FirRegularClassSymbol
                ?: return@lazy null
        val irSymbol = firSymbol.toIrSymbol(c, ConversionTypeOrigin.DEFAULT) as? IrClassSymbol ?: return@lazy null
        val firConstructorSymbol = firSymbol.unsubstitutedScope(c).getDeclaredConstructors().singleOrNull() ?: return@lazy null
        val constructorSymbol = c.declarationStorage.getIrConstructorSymbol(firConstructorSymbol)
        return@lazy IrConstructorCallImpl.fromSymbolOwner(irSymbol.defaultType, constructorSymbol)
    }

    val arrayClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Array) }

    private val primitiveTypeToIrType: Map<PrimitiveType, IrType> by lazy {
        mapOf(
            PrimitiveType.BOOLEAN to booleanType,
            PrimitiveType.CHAR to charType,
            PrimitiveType.BYTE to byteType,
            PrimitiveType.SHORT to shortType,
            PrimitiveType.INT to intType,
            PrimitiveType.LONG to longType,
            PrimitiveType.FLOAT to floatType,
            PrimitiveType.DOUBLE to doubleType
        )
    }

    private fun loadPrimitiveArray(primitiveType: PrimitiveType): IrClassSymbol {
        return loadClass(ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("${primitiveType.typeName}Array")))
    }

    val booleanArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.BOOLEAN) }
    val charArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.CHAR) }
    val byteArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.BYTE) }
    val shortArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.SHORT) }
    val intArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.INT) }
    val longArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.LONG) }
    val floatArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.FLOAT) }
    val doubleArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.DOUBLE) }

    private val primitiveArraysToPrimitiveTypes: Map<IrClassSymbol, PrimitiveType> by lazy {
        mapOf(
            booleanArray to PrimitiveType.BOOLEAN,
            charArray to PrimitiveType.CHAR,
            byteArray to PrimitiveType.BYTE,
            shortArray to PrimitiveType.SHORT,
            intArray to PrimitiveType.INT,
            longArray to PrimitiveType.LONG,
            floatArray to PrimitiveType.FLOAT,
            doubleArray to PrimitiveType.DOUBLE
        )
    }

    val primitiveArrayElementTypes: Map<IrClassSymbol, IrType?>
        get() = primitiveArraysToPrimitiveTypes.mapValues { primitiveTypeToIrType[it.value] }

    val primitiveArrayForType: Map<IrType?, IrClassSymbol>
        get() = primitiveArrayElementTypes.asSequence().associate { it.value to it.key }

    val ieee754equalsFunByOperandType: MutableMap<IrClassifierSymbol, IrSimpleFunctionSymbol> get() = TODO()

    val eqeqeqSymbol: IrSimpleFunctionSymbol get() = TODO()
    val eqeqSymbol: IrSimpleFunctionSymbol get() = TODO()
    val noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol get() = TODO()

    val checkNotNullSymbol: IrSimpleFunctionSymbol get() = TODO()

    val lessFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> get() = TODO()
    val lessOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> get() = TODO()
    val greaterOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> get() = TODO()
    val greaterFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> get() = TODO()

    private val unsignedTypesToUnsignedArrays: Map<UnsignedType, IrClassSymbol> by lazy {
        UnsignedType.entries.mapNotNull { unsignedType ->
            val array = loadClassSafe(unsignedType.arrayClassId)
            if (array == null) null else unsignedType to array
        }.toMap()
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    val unsignedArraysElementTypes: Map<IrClassSymbol, IrType?> by lazy {
        unsignedTypesToUnsignedArrays.map { (k, v) -> v to loadClass(k.classId).owner.defaultType }.toMap()
    }

    fun getNonBuiltInFunctionsWithFirCounterpartByExtensionReceiver(
        name: Name,
        vararg packageNameSegments: String,
    ): Map<IrClassifierSymbol, Pair<FirNamedFunctionSymbol, IrSimpleFunctionSymbol>> {
        return getFunctionsByKey(
            name,
            *packageNameSegments,
            mapKey = { symbol ->
                symbol.fir.receiverParameter?.typeRef?.toIrType(c)?.classifierOrNull
            },
            mapValue = { firSymbol, irSymbol -> firSymbol to irSymbol }
        )
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    // TODO: candidate for removal
    fun findBuiltInClassMemberFunctions(builtInClass: IrClassSymbol, name: Name): Iterable<IrSimpleFunctionSymbol> {
        return builtInClass.functions.filter { it.owner.name == name }.asIterable()
    }

// ---------------

    private fun loadClass(classId: ClassId): IrClassSymbol {
        return loadClassSafe(classId) ?: error("Class not found: $classId")
    }

    private fun loadClassSafe(classId: ClassId): IrClassSymbol? {
        val firClassSymbol = symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: return null
        return c.classifierStorage.getIrClassSymbol(firClassSymbol)
    }

    private fun findFunctions(packageName: FqName, name: Name): List<IrSimpleFunctionSymbol> {
        return symbolProvider.getTopLevelFunctionSymbols(packageName, name).map { findFunction(it) }
    }

    private inline fun <K : Any, T> getFunctionsByKey(
        name: Name,
        vararg packageNameSegments: String,
        mapKey: (FirNamedFunctionSymbol) -> K?,
        mapValue: (FirNamedFunctionSymbol, IrSimpleFunctionSymbol) -> T
    ): Map<K, T> {
        val packageName = FqName.fromSegments(packageNameSegments.asList())
        val result = mutableMapOf<K, T>()
        for (functionSymbol in symbolProvider.getTopLevelFunctionSymbols(packageName, name)) {
            val key = mapKey(functionSymbol) ?: continue
            val irFunctionSymbol = findFunction(functionSymbol)
            result[key] = mapValue(functionSymbol, irFunctionSymbol)
        }
        return result
    }

    private fun findFunction(functionSymbol: FirNamedFunctionSymbol): IrSimpleFunctionSymbol {
        functionSymbol.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        return c.declarationStorage.getIrFunctionSymbol(functionSymbol) as IrSimpleFunctionSymbol
    }

    private val IrClassSymbol.defaultTypeWithoutArguments: IrSimpleType
        get() = IrSimpleTypeImpl(
            kotlinType = null,
            classifier = this,
            nullability = SimpleTypeNullability.DEFINITELY_NOT_NULL,
            arguments = emptyList(),
            annotations = emptyList()
        )
}
