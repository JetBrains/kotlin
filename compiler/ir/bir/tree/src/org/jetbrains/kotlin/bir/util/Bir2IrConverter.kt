/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(IrImplementationDetail::class)

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.types.IrUninitializedType
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.utils.memoryOptimizedMap

@OptIn(ObsoleteDescriptorBasedAPI::class)
class Bir2IrConverter(
    dynamicPropertyManager: BirDynamicPropertiesManager,
    compressedSourceSpanManager: CompressedSourceSpanManager,
    remappedIr2BirElements: Map<BirElement, IrElement>,
    private val irBuiltIns: IrBuiltIns,
    compiledBir: BirDatabase,
    expectedTreeSize: Int = 0,
) : Bir2IrConverterBase(dynamicPropertyManager, compressedSourceSpanManager, remappedIr2BirElements, compiledBir) {
    private val modules = createElementMap<IrModuleFragment, BirModuleFragment>(1)
    private val classes = createElementMap<IrClass, BirClass>((expectedTreeSize * 0.004).toInt())
    private val scripts = createElementMap<IrScript, BirScript>()
    private val constructors = createElementMap<IrConstructor, BirConstructor>((expectedTreeSize * 0.04).toInt())
    private val enumEntries = createElementMap<IrEnumEntry, BirEnumEntry>()
    private val externalPackageFragments = createElementMap<IrExternalPackageFragment, BirExternalPackageFragment>()
    private val fields = createElementMap<IrField, BirField>((expectedTreeSize * 0.02).toInt())
    private val files = createElementMap<IrFile, BirFile>()
    private val functions = createElementMap<IrSimpleFunction, BirSimpleFunction>((expectedTreeSize * 0.15).toInt())
    private val properties = createElementMap<IrProperty, BirProperty>((expectedTreeSize * 0.07).toInt())
    private val returnableBlocks = createElementMap<IrReturnableBlock, BirReturnableBlock>()
    private val typeParameters = createElementMap<IrTypeParameter, BirTypeParameter>((expectedTreeSize * 0.01).toInt())
    private val valueParameters = createElementMap<IrValueParameter, BirValueParameter>((expectedTreeSize * 0.2).toInt())
    private val variables = createElementMap<IrVariable, BirVariable>((expectedTreeSize * 0.02).toInt())
    private val localDelegatedProperties = createElementMap<IrLocalDelegatedProperty, BirLocalDelegatedProperty>()
    private val typeAliases = createElementMap<IrTypeAlias, BirTypeAlias>()
    private val loops = createElementMap<IrLoop, BirLoop>()

    @Suppress("UNCHECKED_CAST")
    override fun <Ir : IrElement> copyElement(old: BirElement): Ir = when (old) {
        is BirValueParameter -> copyValueParameter(old)
        is BirClass -> copyClass(old)
        is BirAnonymousInitializer -> copyAnonymousInitializer(old)
        is BirTypeParameter -> copyTypeParameter(old)
        is BirConstructor -> copyConstructor(old)
        is BirEnumEntry -> copyEnumEntry(old)
        is BirErrorDeclaration -> copyErrorDeclaration(old)
        is BirField -> copyField(old)
        is BirLocalDelegatedProperty -> copyLocalDelegatedProperty(old)
        is BirModuleFragment -> copyModuleFragment(old)
        is BirProperty -> copyProperty(old)
        is BirScript -> copyScript(old)
        is BirSimpleFunction -> copySimpleFunction(old)
        is BirTypeAlias -> copyTypeAlias(old)
        is BirVariable -> copyVariable(old)
        is BirExternalPackageFragment -> copyExternalPackageFragment(old)
        is BirFile -> copyFile(old)
        is BirExpressionBody -> copyExpressionBody(old)
        is BirBlockBody -> copyBlockBody(old)
        is BirConstructorCall -> copyConstructorCall(old)
        is BirGetObjectValue -> copyGetObjectValue(old)
        is BirGetEnumValue -> copyGetEnumValue(old)
        is BirRawFunctionReference -> copyRawFunctionReference(old)
        is BirComposite -> copyComposite(old)
        is BirReturnableBlock -> copyReturnableBlock(old)
        is BirInlinedFunctionBlock -> copyInlinedFunctionBlock(old)
        is BirBlock -> copyBlock(old)
        is BirSyntheticBody -> copySyntheticBody(old)
        is BirBreak -> copyBreak(old)
        is BirContinue -> copyContinue(old)
        is BirCall -> copyCall(old)
        is BirFunctionReference -> copyFunctionReference(old)
        is BirPropertyReference -> copyPropertyReference(old)
        is BirLocalDelegatedPropertyReference -> copyLocalDelegatedPropertyReference(old)
        is BirClassReference -> copyClassReference(old)
        is BirConst<*> -> copyConst(old)
        is BirConstantPrimitive -> copyConstantPrimitive(old)
        is BirConstantObject -> copyConstantObject(old)
        is BirConstantArray -> copyConstantArray(old)
        is BirDelegatingConstructorCall -> copyDelegatingConstructorCall(old)
        is BirDynamicOperatorExpression -> copyDynamicOperatorExpression(old)
        is BirDynamicMemberExpression -> copyDynamicMemberExpression(old)
        is BirEnumConstructorCall -> copyEnumConstructorCall(old)
        is BirErrorCallExpression -> copyErrorCallExpression(old)
        is BirGetField -> copyGetField(old)
        is BirSetField -> copySetField(old)
        is BirFunctionExpression -> copyFunctionExpression(old)
        is BirGetClass -> copyGetClass(old)
        is BirInstanceInitializerCall -> copyInstanceInitializerCall(old)
        is BirWhileLoop -> copyWhileLoop(old)
        is BirDoWhileLoop -> copyDoWhileLoop(old)
        is BirReturn -> copyReturn(old)
        is BirStringConcatenation -> copyStringConcatenation(old)
        is BirSuspensionPoint -> copySuspensionPoint(old)
        is BirSuspendableExpression -> copySuspendableExpression(old)
        is BirThrow -> copyThrow(old)
        is BirTry -> copyTry(old)
        is BirCatch -> copyCatch(old)
        is BirTypeOperatorCall -> copyTypeOperatorCall(old)
        is BirGetValue -> copyGetValue(old)
        is BirSetValue -> copySetValue(old)
        is BirVararg -> copyVararg(old)
        is BirSpreadElement -> copySpreadElement(old)
        is BirWhen -> copyWhen(old)
        is BirElseBranch -> copyElseBranch(old)
        is BirBranch -> copyBranch(old)
        is BirErrorExpression -> copyErrorExpression(old)
        else -> error(old)
    } as Ir

    private fun copyValueParameter(old: BirValueParameter): IrValueParameter = copyReferencedElement(old, valueParameters, {
        IrValueParameterImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            origin = old.origin,
            name = old.name,
            type = IrUninitializedType,
            isAssignable = old.isAssignable,
            varargElementType = null,
            isCrossinline = old.isCrossinline,
            isNoinline = old.isNoinline,
            isHidden = old.isHidden,
            index = old.index,
            symbol = createBindableSymbol(old.symbol),
            factory = IrFactoryImpl,
        )
    }) {
        defaultValue = old.defaultValue?.let { copyChildElement(it) }
        annotations = old.annotations.map { copyChildElement(it) }
        type = remapType(old.type)
        varargElementType = old.varargElementType?.let { remapType(it) }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        origin = old.origin
        name = old.name
        isCrossinline = old.isCrossinline
        isNoinline = old.isNoinline
        isHidden = old.isHidden
        index = old.index
    }

    private fun copyClass(old: BirClass): IrClass = copyReferencedElement(old, classes, {
        IrClassImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            origin = old.origin,
            visibility = old.visibility,
            name = old.name,
            kind = old.kind,
            modality = old.modality,
            source = old.source,
            symbol = createBindableSymbol(old.symbol),
            factory = IrFactoryImpl,
        ).apply {
            isExternal = old.isExternal
            isCompanion = old.isCompanion
            isInner = old.isInner
            isData = old.isData
            isValue = old.isValue
            isExpect = old.isExpect
            isFun = old.isFun
            hasEnumEntries = old.hasEnumEntries
        }
    }) {
        copyAttributes(old)
        thisReceiver = old.thisReceiver?.let { copyChildElement<IrValueParameter>(it) }
        typeParameters = old.typeParameters.map { copyChildElement(it) }
        old.declarations.copyTo(declarations) { copyChildElement(it) }
        annotations = old.annotations.map { copyChildElement(it) }
        superTypes = old.superTypes.memoryOptimizedMap { remapType(it) }
        valueClassRepresentation = old.valueClassRepresentation?.mapUnderlyingType { remapType(it) as IrSimpleType }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        origin = old.origin
        visibility = old.visibility
        name = old.name
        isExternal = old.isExternal
        kind = old.kind
        modality = old.modality
        isCompanion = old.isCompanion
        isInner = old.isInner
        isData = old.isData
        isValue = old.isValue
        isExpect = old.isExpect
        isFun = old.isFun
        hasEnumEntries = old.hasEnumEntries
        assert(source == old.source)
    }

    private fun copyAnonymousInitializer(old: BirAnonymousInitializer): IrAnonymousInitializer = copyNotReferencedElement(old, {
        IrAnonymousInitializerImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            origin = old.origin,
            isStatic = old.isStatic,
            symbol = createBindableSymbol(old.symbol),
            factory = IrFactoryImpl,
        )
    }) {
        copyDynamicProperties(old)
        annotations = old.annotations.map { copyChildElement(it) }
        body = copyChildElement(old.body)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
    }

    private fun copyTypeParameter(old: BirTypeParameter): IrTypeParameter = copyReferencedElement(old, typeParameters, {
        IrTypeParameterImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            origin = old.origin,
            name = old.name,
            variance = old.variance,
            isReified = old.isReified,
            index = old.index,
            symbol = createBindableSymbol(old.symbol),
            factory = IrFactoryImpl,
        )
    }) {
        annotations = old.annotations.map { copyChildElement(it) }
        superTypes = old.superTypes.memoryOptimizedMap { remapType(it) }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        origin = old.origin
        name = old.name
        variance = old.variance
        isReified = old.isReified
        index = old.index
    }

    private fun copyConstructor(old: BirConstructor): IrConstructor = copyReferencedElement(old, constructors, {
        IrConstructorImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            origin = old.origin,
            visibility = old.visibility,
            name = old.name,
            isExternal = old.isExternal,
            isInline = old.isInline,
            isExpect = old.isExpect,
            isPrimary = old.isPrimary,
            symbol = createBindableSymbol(old.symbol),
            containerSource = old[GlobalBirDynamicProperties.ContainerSource],
            factory = IrFactoryImpl,
        )
    }) {
        dispatchReceiverParameter = old.dispatchReceiverParameter?.let { copyChildElement(it) }
        extensionReceiverParameter = old.extensionReceiverParameter?.let { copyChildElement(it) }
        valueParameters = old.valueParameters.map { copyChildElement(it) }
        body = old.body?.let { copyChildElement(it) }
        typeParameters = old.typeParameters.map { copyChildElement(it) }
        annotations = old.annotations.map { copyChildElement(it) }
        returnType = remapType(old.returnType)
        contextReceiverParametersCount = old.contextReceiverParametersCount
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        origin = old.origin
        visibility = old.visibility
        name = old.name
        isExternal = old.isExternal
        isInline = old.isInline
        isExpect = old.isExpect
        isPrimary = old.isPrimary
    }

    private fun copyEnumEntry(old: BirEnumEntry): IrEnumEntry = copyReferencedElement(old, enumEntries, {
        IrEnumEntryImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            origin = old.origin,
            name = old.name,
            symbol = createBindableSymbol(old.symbol),
            factory = IrFactoryImpl,
        )
    }) {
        initializerExpression = old.initializerExpression?.let { copyChildElement(it) }
        correspondingClass = old.correspondingClass?.let { copyChildElement(it) }
        annotations = old.annotations.map { copyChildElement(it) }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        origin = old.origin
        name = old.name
    }

    private fun copyErrorDeclaration(old: BirErrorDeclaration): IrErrorDeclaration = copyNotReferencedElement(old, {
        IrErrorDeclarationImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            origin = old.origin,
            factory = IrFactoryImpl,
        ).apply {
            old[GlobalBirDynamicProperties.Descriptor]?.let {
                descriptor = it
            }
        }
    }) {
        origin = old.origin
        copyDynamicProperties(old)
        annotations = old.annotations.map { copyChildElement(it) }

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
    }

    private fun copyField(old: BirField): IrField = copyReferencedElement(old, fields, {
        IrFieldImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            origin = old.origin,
            visibility = old.visibility,
            name = old.name,
            isExternal = old.isExternal,
            type = IrUninitializedType,
            isFinal = old.isFinal,
            isStatic = old.isStatic,
            symbol = createBindableSymbol(old.symbol),
            factory = IrFactoryImpl,
        )
    }) {
        initializer = old.initializer?.let { copyChildElement(it) }
        correspondingPropertySymbol = old.correspondingPropertySymbol?.let { remapSymbol(it) }
        annotations = old.annotations.map { copyChildElement(it) }
        type = remapType(old.type)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        origin = old.origin
        visibility = old.visibility
        name = old.name
        isExternal = old.isExternal
        isFinal = old.isFinal
        isStatic = old.isStatic
    }

    private fun copyLocalDelegatedProperty(old: BirLocalDelegatedProperty): IrLocalDelegatedProperty =
        copyReferencedElement(old, localDelegatedProperties, {
            IrLocalDelegatedPropertyImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                origin = old.origin,
                name = old.name,
                type = IrUninitializedType,
                isVar = old.isVar,
                symbol = createBindableSymbol(old.symbol),
                factory = IrFactoryImpl,
            )
        }) {
            setter = old.setter?.let { copyChildElement(it) }
            annotations = old.annotations.map { copyChildElement(it) }
            type = remapType(old.type)
            copyDynamicProperties(old)
            delegate = copyChildElement(old.delegate)
            getter = copyChildElement(old.getter)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            origin = old.origin
            name = old.name
            isVar = old.isVar
        }

    private fun copyModuleFragment(old: BirModuleFragment): IrModuleFragment = copyReferencedElement(old, modules, {
        IrModuleFragmentImpl(
            descriptor = old.descriptor,
            irBuiltins = irBuiltIns,
        )
    }) {
        old.files.copyTo(files) { copyChildElement(it) }
        copyDynamicProperties(old)
    }

    private fun copyProperty(old: BirProperty): IrProperty = copyReferencedElement(old, properties, {
        IrPropertyImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            origin = old.origin,
            name = old.name,
            isExternal = old.isExternal,
            visibility = old.visibility,
            modality = old.modality,
            isFakeOverride = old.isFakeOverride,
            isVar = old.isVar,
            isConst = old.isConst,
            isLateinit = old.isLateinit,
            isDelegated = old.isDelegated,
            isExpect = old.isExpect,
            symbol = createBindableSymbol(old.symbol),
            containerSource = old[GlobalBirDynamicProperties.ContainerSource],
            factory = IrFactoryImpl,
        )
    }) {
        copyAttributes(old)
        backingField = old.backingField?.let { copyChildElement(it) }
        getter = old.getter?.let { copyChildElement(it) }
        setter = old.setter?.let { copyChildElement(it) }
        overriddenSymbols = old.overriddenSymbols.memoryOptimizedMap { remapSymbol(it) }
        annotations = old.annotations.map { copyChildElement(it) }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        origin = old.origin
        name = old.name
        isExternal = old.isExternal
        visibility = old.visibility
        modality = old.modality
        isFakeOverride = old.isFakeOverride
        isVar = old.isVar
        isConst = old.isConst
        isLateinit = old.isLateinit
        isDelegated = old.isDelegated
        isExpect = old.isExpect
    }

    private fun copyScript(old: BirScript): IrScript = copyReferencedElement(old, scripts, {
        IrScriptImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            name = old.name,
            symbol = createBindableSymbol(old.symbol),
            factory = IrFactoryImpl,
        )
    }) {
        thisReceiver = old.thisReceiver?.let { copyChildElement(it) }
        explicitCallParameters = old.explicitCallParameters.map { copyChildElement(it) }
        implicitReceiversParameters = old.implicitReceiversParameters.map { copyChildElement(it) }
        providedPropertiesParameters = old.providedPropertiesParameters.map { copyChildElement(it) }
        earlierScriptsParameter = old.earlierScriptsParameter?.let { copyChildElement(it) }
        constructor = old.constructor?.let { remapElement(it) }
        old.statements.copyTo(statements) { copyChildElement(it) }
        annotations = old.annotations.map { copyChildElement(it) }
        baseClass = old.baseClass?.let { remapType(it) }
        origin = old.origin
        providedProperties = old.providedProperties.memoryOptimizedMap { remapSymbol(it) }
        resultProperty = old.resultProperty?.let { remapSymbol(it) }
        earlierScripts = old.earlierScripts?.memoryOptimizedMap { remapSymbol(it) }
        targetClass = old.targetClass?.let { remapSymbol(it) }
        importedScripts = old.importedScripts?.memoryOptimizedMap { remapSymbol(it) }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        name = old.name
    }

    private fun copySimpleFunction(old: BirSimpleFunction): IrSimpleFunction = copyReferencedElement(old, functions, {
        IrFunctionImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            origin = old.origin,
            visibility = old.visibility,
            name = old.name,
            isExternal = old.isExternal,
            isInline = old.isInline,
            isExpect = old.isExpect,
            modality = old.modality,
            isFakeOverride = old.isFakeOverride,
            isTailrec = old.isTailrec,
            isSuspend = old.isSuspend,
            isOperator = old.isOperator,
            isInfix = old.isInfix,
            symbol = createBindableSymbol(old.symbol),
            containerSource = old[GlobalBirDynamicProperties.ContainerSource],
            factory = IrFactoryImpl,
        )
    }) {
        copyAttributes(old)
        dispatchReceiverParameter = old.dispatchReceiverParameter?.let { copyChildElement(it) }
        extensionReceiverParameter = old.extensionReceiverParameter?.let { copyChildElement(it) }
        valueParameters = old.valueParameters.map { copyChildElement(it) }
        body = old.body?.let { copyChildElement(it) }
        typeParameters = old.typeParameters.map { copyChildElement(it) }
        overriddenSymbols = old.overriddenSymbols.memoryOptimizedMap { remapSymbol(it) }
        correspondingPropertySymbol = old.correspondingPropertySymbol?.let { remapSymbol(it) }
        annotations = old.annotations.map { copyChildElement(it) }
        returnType = remapType(old.returnType)
        contextReceiverParametersCount = old.contextReceiverParametersCount
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        origin = old.origin
        visibility = old.visibility
        name = old.name
        isExternal = old.isExternal
        isInline = old.isInline
        isExpect = old.isExpect
        modality = old.modality
        isFakeOverride = old.isFakeOverride
        isTailrec = old.isTailrec
        isSuspend = old.isSuspend
        isOperator = old.isOperator
        isInfix = old.isInfix
    }

    private fun copyTypeAlias(old: BirTypeAlias): IrTypeAlias = copyReferencedElement(old, typeAliases, {
        IrTypeAliasImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            origin = old.origin,
            name = old.name,
            visibility = old.visibility,
            isActual = old.isActual,
            expandedType = IrUninitializedType,
            symbol = createBindableSymbol(old.symbol),
            factory = IrFactoryImpl,
        )
    }) {
        typeParameters = old.typeParameters.map { copyChildElement(it) }
        annotations = old.annotations.map { copyChildElement(it) }
        expandedType = remapType(old.expandedType)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        origin = old.origin
        name = old.name
        visibility = old.visibility
        isActual = old.isActual
    }

    private fun copyVariable(old: BirVariable): IrVariable = copyReferencedElement(old, variables, {
        IrVariableImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            origin = old.origin,
            name = old.name,
            type = IrUninitializedType,
            isVar = old.isVar,
            isConst = old.isConst,
            isLateinit = old.isLateinit,
            symbol = createBindableSymbol(old.symbol),
        )
    }) {
        initializer = old.initializer?.let { copyChildElement(it) }
        annotations = old.annotations.map { copyChildElement(it) }
        type = remapType(old.type)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        origin = old.origin
        name = old.name
        isVar = old.isVar
        isConst = old.isConst
        isLateinit = old.isLateinit
    }

    private fun copyExternalPackageFragment(old: BirExternalPackageFragment): IrExternalPackageFragment =
        copyReferencedElement(old, externalPackageFragments, {
            IrExternalPackageFragmentImpl(
                symbol = createBindableSymbol(old.symbol),
                packageFqName = old.packageFqName,
            )
        }) {
            old.declarations.copyTo(declarations) { copyChildElement(it) }
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            packageFqName = old.packageFqName
        }

    private fun copyFile(old: BirFile): IrFile = copyReferencedElement(old, files, {
        IrFileImpl(
            packageFqName = old.packageFqName,
            fileEntry = old.fileEntry,
            symbol = createBindableSymbol(old.symbol),
        )
    }) {
        old.declarations.copyTo(declarations) { copyChildElement(it) }
        annotations = old.annotations.map { copyChildElement(it) }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        packageFqName = old.packageFqName
        fileEntry = old.fileEntry
    }

    private fun copyExpressionBody(old: BirExpressionBody): IrExpressionBody = copyNotReferencedElement(old, {
        IrFactoryImpl.createExpressionBody(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            expression = copyChildElement(old.expression),
        )
    }) {
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        expression = copyChildElement(old.expression)
    }

    private fun copyBlockBody(old: BirBlockBody): IrBlockBody = copyNotReferencedElement(old, {
        IrFactoryImpl.createBlockBody(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
        )
    }) {
        old.statements.copyTo(statements) { copyChildElement(it) }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
    }

    private fun copyConstructorCall(old: BirConstructorCall): IrConstructorCall = copyNotReferencedElement(old, {
        IrConstructorCallImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            symbol = remapSymbol(old.symbol),
            origin = old.origin,
            source = old.source,
            constructorTypeArgumentsCount = old.constructorTypeArgumentsCount,
            typeArgumentsCount = old.typeArguments.size,
            valueArgumentsCount = old.valueArguments.size,
        )
    }) {
        copyAttributes(old)
        copyIrMemberAccessExpressionArguments(old)
        dispatchReceiver = old.dispatchReceiver?.let { copyChildElement(it) }
        extensionReceiver = old.extensionReceiver?.let { copyChildElement(it) }
        contextReceiversCount = old.contextReceiversCount
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        origin = old.origin
        source = old.source
        symbol = remapSymbol(old.symbol)
    }

    private fun copyGetObjectValue(old: BirGetObjectValue): IrGetObjectValue = copyNotReferencedElement(old, {
        IrGetObjectValueImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            symbol = remapSymbol(old.symbol),
        )
    }) {
        copyAttributes(old)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        symbol = remapSymbol(old.symbol)
    }

    private fun copyGetEnumValue(old: BirGetEnumValue): IrGetEnumValue = copyNotReferencedElement(old, {
        IrGetEnumValueImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            symbol = remapSymbol(old.symbol),
        )
    }) {
        copyAttributes(old)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        symbol = remapSymbol(old.symbol)
    }

    private fun copyRawFunctionReference(old: BirRawFunctionReference): IrRawFunctionReference = copyNotReferencedElement(old, {
        IrRawFunctionReferenceImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            symbol = remapSymbol(old.symbol),
        )
    }) {
        copyAttributes(old)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        symbol = remapSymbol(old.symbol)
    }

    private fun copyBlock(old: BirBlock): IrBlock = copyNotReferencedElement(old, {
        IrBlockImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            origin = old.origin,
        )
    }) {
        copyAttributes(old)
        old.statements.copyTo(statements) { copyChildElement(it) }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        origin = old.origin
    }

    private fun copyComposite(old: BirComposite): IrComposite = copyNotReferencedElement(old, {
        IrCompositeImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            origin = old.origin,
        )
    }) {
        copyAttributes(old)
        old.statements.copyTo(statements) { copyChildElement(it) }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        origin = old.origin
    }

    private fun copyReturnableBlock(old: BirReturnableBlock): IrReturnableBlock = copyReferencedElement(old, returnableBlocks, {
        IrReturnableBlockImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            origin = old.origin,
            symbol = createBindableSymbol(old.symbol),
        )
    }) {
        copyAttributes(old)
        old.statements.copyTo(statements) { copyChildElement(it) }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        origin = old.origin
        type = remapType(old.type)
    }

    private fun copyInlinedFunctionBlock(old: BirInlinedFunctionBlock): IrInlinedFunctionBlock = copyNotReferencedElement(old, {
        IrInlinedFunctionBlockImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            origin = old.origin,
            inlineCall = remapElement(old.inlineCall),
            inlinedElement = remapElement(old.inlinedElement),
        )
    }) {
        copyAttributes(old)
        old.statements.copyTo(statements) { copyChildElement(it) }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        origin = old.origin
        inlineCall = remapElement(old.inlineCall)
        inlinedElement = remapElement(old.inlinedElement)
    }

    private fun copySyntheticBody(old: BirSyntheticBody): IrSyntheticBody = copyNotReferencedElement(old, {
        IrSyntheticBodyImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            kind = old.kind,
        )
    }) {
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        kind = old.kind
    }

    private fun copyBreak(old: BirBreak): IrBreak = copyNotReferencedElement(old, {
        IrBreakImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            loop = remapElement(old.loop),
        )
    }) {
        copyAttributes(old)
        label = old.label
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        loop = remapElement(old.loop)
    }

    private fun copyContinue(old: BirContinue): IrContinue = copyNotReferencedElement(old, {
        IrContinueImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            loop = remapElement(old.loop),
        )
    }) {
        copyAttributes(old)
        label = old.label
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        loop = remapElement(old.loop)
    }

    private fun copyCall(old: BirCall): IrCall = copyNotReferencedElement(old, {
        IrCallImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            symbol = remapSymbol(old.symbol),
            origin = old.origin,
            superQualifierSymbol = old.superQualifierSymbol?.let { remapSymbol(it) },
            valueArgumentsCount = old.valueArguments.size,
            typeArgumentsCount = old.typeArguments.size,
        )
    }) {
        copyAttributes(old)
        copyIrMemberAccessExpressionArguments(old)
        dispatchReceiver = old.dispatchReceiver?.let { copyChildElement(it) }
        extensionReceiver = old.extensionReceiver?.let { copyChildElement(it) }
        contextReceiversCount = old.contextReceiversCount
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        origin = old.origin
        superQualifierSymbol = old.superQualifierSymbol?.let { remapSymbol(it) }
        symbol = remapSymbol(old.symbol)
    }

    private fun copyFunctionReference(old: BirFunctionReference): IrFunctionReference = copyNotReferencedElement(old, {
        IrFunctionReferenceImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            symbol = remapSymbol(old.symbol),
            origin = old.origin,
            reflectionTarget = old.reflectionTarget?.let { remapSymbol(it) },
            valueArgumentsCount = old.valueArguments.size,
            typeArgumentsCount = old.typeArguments.size,
        )
    }) {
        copyAttributes(old)
        dispatchReceiver = old.dispatchReceiver?.let { copyChildElement(it) }
        extensionReceiver = old.extensionReceiver?.let { copyChildElement(it) }
        copyIrMemberAccessExpressionArguments(old)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        origin = old.origin
        reflectionTarget = old.reflectionTarget?.let { remapSymbol(it) }
        symbol = remapSymbol(old.symbol)
    }

    private fun copyPropertyReference(old: BirPropertyReference): IrPropertyReference = copyNotReferencedElement(old, {
        IrPropertyReferenceImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            symbol = remapSymbol(old.symbol),
            origin = old.origin,
            field = old.field?.let { remapSymbol(it) },
            getter = old.getter?.let { remapSymbol(it) },
            setter = old.setter?.let { remapSymbol(it) },
            typeArgumentsCount = old.typeArguments.size,
        )
    }) {
        copyAttributes(old)
        dispatchReceiver = old.dispatchReceiver?.let { copyChildElement(it) }
        extensionReceiver = old.extensionReceiver?.let { copyChildElement(it) }
        copyIrMemberAccessExpressionArguments(old)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        origin = old.origin
        field = old.field?.let { remapSymbol(it) }
        getter = old.getter?.let { remapSymbol(it) }
        setter = old.setter?.let { remapSymbol(it) }
        symbol = remapSymbol(old.symbol)
    }

    private fun copyLocalDelegatedPropertyReference(old: BirLocalDelegatedPropertyReference): IrLocalDelegatedPropertyReference =
        copyNotReferencedElement(old, {
            IrLocalDelegatedPropertyReferenceImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                origin = old.origin,
                symbol = remapSymbol(old.symbol),
                delegate = remapSymbol(old.delegate),
                getter = remapSymbol(old.getter),
                setter = old.setter?.let { remapSymbol(it) },
            )
        }) {
            copyAttributes(old)
            dispatchReceiver = old.dispatchReceiver?.let { copyChildElement(it) }
            extensionReceiver = old.extensionReceiver?.let { copyChildElement(it) }
            copyIrMemberAccessExpressionArguments(old)
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            origin = old.origin
            delegate = remapSymbol(old.delegate)
            getter = remapSymbol(old.getter)
            setter = old.setter?.let { remapSymbol(it) }
            symbol = remapSymbol(old.symbol)
        }

    private fun copyClassReference(old: BirClassReference): IrClassReference = copyNotReferencedElement(old, {
        IrClassReferenceImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            symbol = remapSymbol(old.symbol),
            classType = IrUninitializedType,
        )
    }) {
        copyAttributes(old)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        classType = remapType(old.type)
        symbol = remapSymbol(old.symbol)
    }

    private fun <T> copyConst(old: BirConst<T>): IrConst<T> = copyNotReferencedElement(old, {
        IrConstImpl<T>(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            kind = old.kind,
            value = old.value,
        )
    }) {
        copyAttributes(old)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        kind = old.kind
        value = old.value
    }

    private fun copyConstantPrimitive(old: BirConstantPrimitive): IrConstantPrimitive = copyNotReferencedElement(old, {
        IrConstantPrimitiveImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            value = copyChildElement(old.value),
        )
    }) {
        copyAttributes(old)
        type = remapType(old.type)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        value = copyChildElement(old.value)
    }

    private fun copyConstantObject(old: BirConstantObject): IrConstantObject = copyNotReferencedElement(old, {
        IrConstantObjectImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            constructor = remapSymbol(old.constructor),
            initValueArguments = emptyList(),
            initTypeArguments = old.typeArguments.memoryOptimizedMap { remapType(it) },
        )
    }) {
        copyAttributes(old)
        old.valueArguments.copyTo(valueArguments) { copyChildElement(it) }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        constructor = remapSymbol(old.constructor)
    }

    private fun copyConstantArray(old: BirConstantArray): IrConstantArray = copyNotReferencedElement(old, {
        IrConstantArrayImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            initElements = emptyList(),
        )
    }) {
        copyAttributes(old)
        old.elements.copyTo(elements) { copyChildElement(it) }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
    }

    private fun copyDelegatingConstructorCall(old: BirDelegatingConstructorCall): IrDelegatingConstructorCall =
        copyNotReferencedElement(old, {
            IrDelegatingConstructorCallImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                symbol = remapSymbol(old.symbol),
                valueArgumentsCount = old.valueArguments.size,
                typeArgumentsCount = old.typeArguments.size,
            )
        }) {
            copyAttributes(old)
            dispatchReceiver = old.dispatchReceiver?.let { copyChildElement(it) }
            extensionReceiver = old.extensionReceiver?.let { copyChildElement(it) }
            origin = old.origin
            contextReceiversCount = old.contextReceiversCount
            copyIrMemberAccessExpressionArguments(old)
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            symbol = remapSymbol(old.symbol)
        }

    private fun copyDynamicOperatorExpression(old: BirDynamicOperatorExpression): IrDynamicOperatorExpression =
        copyNotReferencedElement(old, {
            IrDynamicOperatorExpressionImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                operator = old.operator,
            )
        }) {
            copyAttributes(old)
            receiver = copyChildElement(old.receiver)
            old.arguments.copyTo(arguments) { copyChildElement(it) }
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            operator = old.operator
        }

    private fun copyDynamicMemberExpression(old: BirDynamicMemberExpression): IrDynamicMemberExpression =
        copyNotReferencedElement(old, {
            IrDynamicMemberExpressionImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                memberName = old.memberName,
                receiver = copyChildElement(old.receiver),
            )
        }) {
            copyAttributes(old)
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            memberName = old.memberName
            receiver = copyChildElement(old.receiver)
        }

    private fun copyEnumConstructorCall(old: BirEnumConstructorCall): IrEnumConstructorCall =
        copyNotReferencedElement(old, {
            IrEnumConstructorCallImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                symbol = remapSymbol(old.symbol),
                typeArgumentsCount = old.typeArguments.size,
                valueArgumentsCount = old.valueArguments.size,
            )
        }) {
            copyAttributes(old)
            origin = old.origin
            dispatchReceiver = old.dispatchReceiver?.let { copyChildElement(it) }
            extensionReceiver = old.extensionReceiver?.let { copyChildElement(it) }
            contextReceiversCount = old.contextReceiversCount
            copyIrMemberAccessExpressionArguments(old)
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            symbol = remapSymbol(old.symbol)
        }

    private fun copyErrorCallExpression(old: BirErrorCallExpression): IrErrorCallExpression =
        copyNotReferencedElement(old, {
            IrErrorCallExpressionImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                description = old.description,
            )
        }) {
            copyAttributes(old)
            explicitReceiver = old.explicitReceiver?.let { copyChildElement(it) }
            old.arguments.copyTo(arguments) { copyChildElement(it) }
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            description = old.description
        }

    private fun copyGetField(old: BirGetField): IrGetField = copyNotReferencedElement(old, {
        IrGetFieldImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            symbol = remapSymbol(old.symbol),
            superQualifierSymbol = old.superQualifierSymbol?.let { remapSymbol(it) },
            receiver = old.receiver?.let { copyChildElement(it) },
            origin = old.origin,
        )
    }) {
        copyAttributes(old)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        superQualifierSymbol = old.superQualifierSymbol?.let { remapSymbol(it) }
        receiver = old.receiver?.let { copyChildElement(it) }
        origin = old.origin
        symbol = remapSymbol(old.symbol)
    }

    private fun copySetField(old: BirSetField): IrSetField = copyNotReferencedElement(old, {
        IrSetFieldImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            symbol = remapSymbol(old.symbol),
            superQualifierSymbol = old.superQualifierSymbol?.let { remapSymbol(it) },
            receiver = old.receiver?.let { copyChildElement(it) },
            origin = old.origin,
            value = copyChildElement(old.value),
        )
    }) {
        copyAttributes(old)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        superQualifierSymbol = old.superQualifierSymbol?.let { remapSymbol(it) }
        receiver = old.receiver?.let { copyChildElement(it) }
        origin = old.origin
        value = copyChildElement(old.value)
        symbol = remapSymbol(old.symbol)
    }

    private fun copyFunctionExpression(old: BirFunctionExpression): IrFunctionExpression =
        copyNotReferencedElement(old, {
            IrFunctionExpressionImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                origin = old.origin,
                function = copyChildElement(old.function),
            )
        }) {
            copyAttributes(old)
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            origin = old.origin
            function = copyChildElement(old.function)
        }

    private fun copyGetClass(old: BirGetClass): IrGetClass = copyNotReferencedElement(old, {
        IrGetClassImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            argument = copyChildElement(old.argument),
        )
    }) {
        copyAttributes(old)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        argument = copyChildElement(old.argument)
    }

    private fun copyInstanceInitializerCall(old: BirInstanceInitializerCall): IrInstanceInitializerCall =
        copyNotReferencedElement(old, {
            IrInstanceInitializerCallImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                classSymbol = remapSymbol(old.classSymbol),
            )
        }) {
            copyAttributes(old)
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            classSymbol = remapSymbol(old.classSymbol)
        }

    private fun copyWhileLoop(old: BirWhileLoop): IrWhileLoop = copyReferencedElement(old, loops, {
        IrWhileLoopImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            origin = old.origin,
        )
    }) {
        copyAttributes(old)
        body = old.body?.let { copyChildElement(it) }
        condition = copyChildElement(old.condition)
        label = old.label
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        origin = old.origin
    }

    private fun copyDoWhileLoop(old: BirDoWhileLoop): IrDoWhileLoop =
        copyReferencedElement(old, loops, {
            IrDoWhileLoopImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                origin = old.origin,
            )
        }) {
            copyAttributes(old)
            body = old.body?.let { copyChildElement(it) }
            condition = copyChildElement(old.condition)
            label = old.label
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            origin = old.origin
        }

    private fun copyReturn(old: BirReturn): IrReturn = copyNotReferencedElement(old, {
        IrReturnImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            value = copyChildElement(old.value),
            returnTargetSymbol = remapSymbol(old.returnTargetSymbol),
        )
    }) {
        copyAttributes(old)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        value = copyChildElement(old.value)
        returnTargetSymbol = remapSymbol(old.returnTargetSymbol)
    }

    private fun copyStringConcatenation(old: BirStringConcatenation): IrStringConcatenation =
        copyNotReferencedElement(old, {
            IrStringConcatenationImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
            )
        }) {
            copyAttributes(old)
            old.arguments.copyTo(arguments) { copyChildElement(it) }
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
        }

    private fun copySuspensionPoint(old: BirSuspensionPoint): IrSuspensionPoint =
        copyNotReferencedElement(old, {
            IrSuspensionPointImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                suspensionPointIdParameter = copyChildElement(old.suspensionPointIdParameter),
                result = copyChildElement(old.result),
                resumeResult = copyChildElement(old.resumeResult),
            )
        }) {
            copyAttributes(old)
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            suspensionPointIdParameter = copyChildElement(old.suspensionPointIdParameter)
            result = copyChildElement(old.result)
            resumeResult = copyChildElement(old.resumeResult)
        }

    private fun copySuspendableExpression(old: BirSuspendableExpression): IrSuspendableExpression =
        copyNotReferencedElement(old, {
            IrSuspendableExpressionImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                suspensionPointId = copyChildElement(old.suspensionPointId),
                result = copyChildElement(old.result),
            )
        }) {
            copyAttributes(old)
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            suspensionPointId = copyChildElement(old.suspensionPointId)
            result = copyChildElement(old.result)
        }

    private fun copyThrow(old: BirThrow): IrThrow = copyNotReferencedElement(old, {
        IrThrowImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
            value = copyChildElement(old.value),
        )
    }) {
        copyAttributes(old)
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
        value = copyChildElement(old.value)
    }

    private fun copyTry(old: BirTry): IrTry = copyNotReferencedElement(old, {
        IrTryImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            type = IrUninitializedType,
        )
    }) {
        copyAttributes(old)
        old.catches.copyTo(catches) { copyChildElement(it) }
        tryResult = copyChildElement(old.tryResult)
        finallyExpression = old.finallyExpression?.let { copyChildElement(it) }
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        type = remapType(old.type)
    }

    private fun copyCatch(old: BirCatch): IrCatch = copyNotReferencedElement(old, {
        IrCatchImpl(
            startOffset = old.sourceSpan.start,
            endOffset = old.sourceSpan.end,
            catchParameter = copyChildElement(old.catchParameter),
            result = copyChildElement(old.result),
        )
    }) {
        copyDynamicProperties(old)

        assert(startOffset == old.sourceSpan.start)
        assert(endOffset == old.sourceSpan.end)
        catchParameter = copyChildElement(old.catchParameter)
        result = copyChildElement(old.result)
    }

    private fun copyTypeOperatorCall(old: BirTypeOperatorCall): IrTypeOperatorCall =
        copyNotReferencedElement(old, {
            IrTypeOperatorCallImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                operator = old.operator,
                argument = copyChildElement(old.argument),
                typeOperand = IrUninitializedType,
            )
        }) {
            copyAttributes(old)
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            operator = old.operator
            argument = copyChildElement(old.argument)
            typeOperand = remapType(old.typeOperand)
        }

    private fun copyGetValue(old: BirGetValue): IrGetValue =
        copyNotReferencedElement(old, {
            IrGetValueImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                symbol = remapSymbol(old.symbol),
                origin = old.origin,
            )
        }) {
            copyAttributes(old)
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            origin = old.origin
            symbol = remapSymbol(old.symbol)
        }

    private fun copySetValue(old: BirSetValue): IrSetValue =
        copyNotReferencedElement(old, {
            IrSetValueImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                symbol = remapSymbol(old.symbol),
                origin = old.origin,
                value = copyChildElement(old.value),
            )
        }) {
            copyAttributes(old)
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            origin = old.origin
            value = copyChildElement(old.value)
            symbol = remapSymbol(old.symbol)
        }

    private fun copyVararg(old: BirVararg): IrVararg =
        copyNotReferencedElement(old, {
            IrVarargImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                varargElementType = IrUninitializedType,
            )
        }) {
            copyAttributes(old)
            old.elements.copyTo(elements) { copyChildElement(it) }
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            varargElementType = remapType(old.varargElementType)
        }

    private fun copySpreadElement(old: BirSpreadElement): IrSpreadElement =
        copyNotReferencedElement(old, {
            IrSpreadElementImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                expression = copyChildElement(old.expression),
            )
        }) {
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            expression = copyChildElement(old.expression)
        }

    private fun copyWhen(old: BirWhen): IrWhen =
        copyNotReferencedElement(old, {
            IrWhenImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                origin = old.origin,
            )
        }) {
            copyAttributes(old)
            old.branches.copyTo(branches) { copyChildElement(it) }
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            origin = old.origin
        }

    private fun copyBranch(old: BirBranch): IrBranch =
        copyNotReferencedElement(old, {
            IrBranchImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                condition = copyChildElement(old.condition),
                result = copyChildElement(old.result),
            )
        }) {
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            condition = copyChildElement(old.condition)
            result = copyChildElement(old.result)
        }

    private fun copyElseBranch(old: BirElseBranch): IrElseBranch =
        copyNotReferencedElement(old, {
            IrElseBranchImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                condition = copyChildElement(old.condition),
                result = copyChildElement(old.result),
            )
        }) {
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            condition = copyChildElement(old.condition)
            result = copyChildElement(old.result)
        }

    private fun copyErrorExpression(old: BirErrorExpression): IrErrorExpression =
        copyNotReferencedElement(old, {
            IrErrorExpressionImpl(
                startOffset = old.sourceSpan.start,
                endOffset = old.sourceSpan.end,
                type = IrUninitializedType,
                description = old.description,
            )
        }) {
            copyDynamicProperties(old)

            assert(startOffset == old.sourceSpan.start)
            assert(endOffset == old.sourceSpan.end)
            type = remapType(old.type)
            description = old.description
        }


    private fun IrElement.copyDynamicProperties(
        from: BirElement,
    ) {
        if (this is IrMetadataSourceOwner) {
            metadata = (from as BirMetadataSourceOwner)[GlobalBirDynamicProperties.Metadata]
        }

        if (this is IrAttributeContainer) {
            originalBeforeInline = (from as BirAttributeContainer)[GlobalBirDynamicProperties.OriginalBeforeInline]
                ?.let { remapElement(it) as IrAttributeContainer }
        }

        if (this is IrClass) {
            sealedSubclasses = (from as BirClass)[GlobalBirDynamicProperties.SealedSubclasses]
                ?.memoryOptimizedMap { remapSymbol(it) } ?: emptyList()
        }
    }
}
