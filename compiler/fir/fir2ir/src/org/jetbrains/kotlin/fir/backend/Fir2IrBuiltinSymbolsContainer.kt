/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.utils.ConversionTypeOrigin
import org.jetbrains.kotlin.fir.backend.utils.defaultTypeWithoutArguments
import org.jetbrains.kotlin.fir.backend.utils.toIrSymbol
import org.jetbrains.kotlin.fir.backend.utils.unsubstitutedScope
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.isBoolean
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

@RequiresOptIn("This declaration can be used only from IrBuiltinsOverFir")
annotation class Fir2IrBuiltInsInternals

@OptIn(Fir2IrBuiltInsInternals::class)
class Fir2IrBuiltinSymbolsContainer(
    private val c: Fir2IrComponents,
    private val syntheticSymbolsContainer: Fir2IrSyntheticIrBuiltinsSymbolsContainer
) {
    private val session: FirSession = c.session

    private val symbolProvider: FirSymbolProvider = session.symbolProvider

    val booleanNotSymbol: IrSimpleFunctionSymbol by lazy {
        val firFunction = findFirMemberFunctions(StandardClassIds.Boolean, OperatorNameConventions.NOT)
            .first { it.resolvedReturnType.isBoolean }
        findFunction(firFunction)
    }

    @Fir2IrBuiltInsInternals
    internal fun findFirMemberFunctions(classId: ClassId, name: Name): List<FirNamedFunctionSymbol> {
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

        IrConstructorCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = IrSimpleTypeImpl(
                classifier = irSymbol,
                nullability = SimpleTypeNullability.DEFINITELY_NOT_NULL,
                arguments = emptyList(),
                annotations = emptyList()
            ),
            constructorSymbol,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
            valueArgumentsCount = 0,
        )
    }

    val arrayClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Array) }

    @Fir2IrBuiltInsInternals
    internal val primitiveSymbolToPrimitiveType: Map<IrClassSymbol, PrimitiveType> by lazy {
        mapOf(
            charClass to PrimitiveType.CHAR,
            byteClass to PrimitiveType.BYTE,
            shortClass to PrimitiveType.SHORT,
            intClass to PrimitiveType.INT,
            longClass to PrimitiveType.LONG,
            floatClass to PrimitiveType.FLOAT,
            doubleClass to PrimitiveType.DOUBLE,
        )
    }

    @Fir2IrBuiltInsInternals
    internal val primitiveTypeToIrType: Map<PrimitiveType, IrType> by lazy {
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

    @Fir2IrBuiltInsInternals
    internal val primitiveArraysToPrimitiveTypes: Map<IrClassSymbol, PrimitiveType> by lazy {
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

    // --------------------------- synthetic symbols ---------------------------

    val eqeqeqSymbol: IrSimpleFunctionSymbol get() = syntheticSymbolsContainer.eqeqeqSymbol
    val eqeqSymbol: IrSimpleFunctionSymbol get() = syntheticSymbolsContainer.eqeqSymbol
    val noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol get() = syntheticSymbolsContainer.noWhenBranchMatchedExceptionSymbol
    val checkNotNullSymbol: IrSimpleFunctionSymbol get() = syntheticSymbolsContainer.checkNotNullSymbol

    val lessFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy {
        operatorMap(syntheticSymbolsContainer.lessFunByOperandType)
    }

    val lessOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy {
        operatorMap(syntheticSymbolsContainer.lessOrEqualFunByOperandType)
    }

    val greaterOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy {
        operatorMap(syntheticSymbolsContainer.greaterOrEqualFunByOperandType)
    }

    val greaterFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy {
        operatorMap(syntheticSymbolsContainer.greaterFunByOperandType)
    }

    val ieee754equalsFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy {
        operatorMap(syntheticSymbolsContainer.ieee754equalsFunByOperandType)
    }

    private fun operatorMap(
        syntheticMap: Map<PrimitiveType, IrSimpleFunctionSymbol>
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> {
        return buildMap {
            for ((classSymbol, type) in primitiveSymbolToPrimitiveType) {
                val functionSymbol = syntheticMap[type] ?: continue
                put(classSymbol, functionSymbol)
            }
        }
    }

    // --------------------------- functions ---------------------------

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

    // --------------------------- utilities for declaration loading ---------------------------

    @Fir2IrBuiltInsInternals
    internal fun loadClass(classId: ClassId): IrClassSymbol {
        return loadClassSafe(classId) ?: error("Class not found: $classId")
    }

    @Fir2IrBuiltInsInternals
    internal fun loadClassSafe(classId: ClassId): IrClassSymbol? {
        val firClassSymbol = symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: return null
        return c.classifierStorage.getIrClassSymbol(firClassSymbol)
    }

    @Fir2IrBuiltInsInternals
    internal fun findFunctions(packageName: FqName, name: Name): List<IrSimpleFunctionSymbol> {
        return symbolProvider.getTopLevelFunctionSymbols(packageName, name).map { findFunction(it) }
    }

    @Fir2IrBuiltInsInternals
    internal inline fun <K : Any, T> getFunctionsByKey(
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

    @Fir2IrBuiltInsInternals
    internal fun findFunction(functionSymbol: FirNamedFunctionSymbol): IrSimpleFunctionSymbol {
        functionSymbol.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        return c.declarationStorage.getIrFunctionSymbol(functionSymbol) as IrSimpleFunctionSymbol
    }

    @Fir2IrBuiltInsInternals
    internal fun findProperties(packageName: FqName, name: Name): List<IrPropertySymbol> {
        return symbolProvider.getTopLevelPropertySymbols(packageName, name).map { findProperty(it) }
    }

    @Fir2IrBuiltInsInternals
    internal fun findProperty(propertySymbol: FirPropertySymbol): IrPropertySymbol {
        propertySymbol.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        return c.declarationStorage.getIrPropertySymbol(propertySymbol) as IrPropertySymbol
    }
}
