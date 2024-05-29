/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.declarations.impl.*
import org.jetbrains.kotlin.bir.declarations.lazy.*
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.expressions.impl.*
import org.jetbrains.kotlin.bir.types.BirUninitializedType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.utils.memoryOptimizedMap

// todo: Could be adjusted for the change that all child fields are now nullable
@OptIn(ObsoleteDescriptorBasedAPI::class)
class Ir2BirConverter(
    dynamicPropertyManager: BirDynamicPropertiesManager,
    compressedSourceSpanManager: CompressedSourceSpanManager,
    expectedTreeSize: Int = 0,
) : Ir2BirConverterBase(compressedSourceSpanManager) {
    private val modules = createElementMap<BirModuleFragment, IrModuleFragment>(1)
    private val classes = createElementMap<BirClass, IrClass>((expectedTreeSize * 0.004).toInt())
    private val scripts = createElementMap<BirScript, IrScript>()
    private val constructors = createElementMap<BirConstructor, IrConstructor>((expectedTreeSize * 0.04).toInt())
    private val enumEntries = createElementMap<BirEnumEntry, IrEnumEntry>()
    private val externalPackageFragments = createElementMap<BirExternalPackageFragment, IrExternalPackageFragment>()
    private val fields = createElementMap<BirField, IrField>((expectedTreeSize * 0.02).toInt())
    private val files = createElementMap<BirFile, IrFile>()
    private val functions = createElementMap<BirSimpleFunction, IrSimpleFunction>((expectedTreeSize * 0.15).toInt())
    private val properties = createElementMap<BirProperty, IrProperty>((expectedTreeSize * 0.07).toInt())
    private val returnableBlocks = createElementMap<BirReturnableBlock, IrReturnableBlock>()
    private val typeParameters = createElementMap<BirTypeParameter, IrTypeParameter>((expectedTreeSize * 0.01).toInt())
    private val valueParameters = createElementMap<BirValueParameter, IrValueParameter>((expectedTreeSize * 0.2).toInt())
    private val variables = createElementMap<BirVariable, IrVariable>((expectedTreeSize * 0.02).toInt())
    private val localDelegatedProperties = createElementMap<BirLocalDelegatedProperty, IrLocalDelegatedProperty>()
    private val typeAliases = createElementMap<BirTypeAlias, IrTypeAlias>()
    private val loops = createElementMap<BirLoop, IrLoop>()

    @Suppress("UNCHECKED_CAST")
    override fun <Bir : BirElement> copyElement(old: IrElement): Bir = when (old) {
        is IrValueParameter -> copyValueParameter(old)
        is IrClass -> copyClass(old)
        is IrAnonymousInitializer -> copyAnonymousInitializer(old)
        is IrTypeParameter -> copyTypeParameter(old)
        is IrConstructor -> copyConstructor(old)
        is IrEnumEntry -> copyEnumEntry(old)
        is IrErrorDeclaration -> copyErrorDeclaration(old)
        is IrField -> copyField(old)
        is IrLocalDelegatedProperty -> copyLocalDelegatedProperty(old)
        is IrModuleFragment -> copyModuleFragment(old)
        is IrProperty -> copyProperty(old)
        is IrScript -> copyScript(old)
        is IrSimpleFunction -> copySimpleFunction(old)
        is IrTypeAlias -> copyTypeAlias(old)
        is IrVariable -> copyVariable(old)
        is IrExternalPackageFragment -> copyExternalPackageFragment(old)
        is IrFile -> copyFile(old)
        is IrExpressionBody -> copyExpressionBody(old)
        is IrBlockBody -> copyBlockBody(old)
        is IrConstructorCall -> copyConstructorCall(old)
        is IrGetObjectValue -> copyGetObjectValue(old)
        is IrGetEnumValue -> copyGetEnumValue(old)
        is IrRawFunctionReference -> copyRawFunctionReference(old)
        is IrComposite -> copyComposite(old)
        is IrReturnableBlock -> copyReturnableBlock(old)
        is IrInlinedFunctionBlock -> copyInlinedFunctionBlock(old)
        is IrBlock -> copyBlock(old)
        is IrSyntheticBody -> copySyntheticBody(old)
        is IrBreak -> copyBreak(old)
        is IrContinue -> copyContinue(old)
        is IrCall -> copyCall(old)
        is IrFunctionReference -> copyFunctionReference(old)
        is IrPropertyReference -> copyPropertyReference(old)
        is IrLocalDelegatedPropertyReference -> copyLocalDelegatedPropertyReference(old)
        is IrClassReference -> copyClassReference(old)
        is IrConst<*> -> copyConst(old)
        is IrConstantPrimitive -> copyConstantPrimitive(old)
        is IrConstantObject -> copyConstantObject(old)
        is IrConstantArray -> copyConstantArray(old)
        is IrDelegatingConstructorCall -> copyDelegatingConstructorCall(old)
        is IrDynamicOperatorExpression -> copyDynamicOperatorExpression(old)
        is IrDynamicMemberExpression -> copyDynamicMemberExpression(old)
        is IrEnumConstructorCall -> copyEnumConstructorCall(old)
        is IrErrorCallExpression -> copyErrorCallExpression(old)
        is IrGetField -> copyGetField(old)
        is IrSetField -> copySetField(old)
        is IrFunctionExpression -> copyFunctionExpression(old)
        is IrGetClass -> copyGetClass(old)
        is IrInstanceInitializerCall -> copyInstanceInitializerCall(old)
        is IrWhileLoop -> copyWhileLoop(old)
        is IrDoWhileLoop -> copyDoWhileLoop(old)
        is IrReturn -> copyReturn(old)
        is IrStringConcatenation -> copyStringConcatenation(old)
        is IrSuspensionPoint -> copySuspensionPoint(old)
        is IrSuspendableExpression -> copySuspendableExpression(old)
        is IrThrow -> copyThrow(old)
        is IrTry -> copyTry(old)
        is IrCatch -> copyCatch(old)
        is IrTypeOperatorCall -> copyTypeOperatorCall(old)
        is IrGetValue -> copyGetValue(old)
        is IrSetValue -> copySetValue(old)
        is IrVararg -> copyVararg(old)
        is IrSpreadElement -> copySpreadElement(old)
        is IrWhen -> copyWhen(old)
        is IrElseBranch -> copyElseBranch(old)
        is IrBranch -> copyBranch(old)
        is IrErrorExpression -> copyErrorExpression(old)
        else -> error(old)
    } as Bir

    private fun copyValueParameter(old: IrValueParameter): BirValueParameter = copyReferencedElement(old, valueParameters, {
        BirValueParameterImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            origin = old.origin,
            name = old.name,
            type = BirUninitializedType,
            isAssignable = old.isAssignable,
            varargElementType = null,
            isCrossinline = old.isCrossinline,
            isNoinline = old.isNoinline,
            isHidden = old.isHidden,
            defaultValue = null,
            index = old.index,
            signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
        )
    }) { new ->
        new.defaultValue = old.defaultValue?.let { copyElement(it) }
        new.type = remapType(old.type)
        new.varargElementType = old.varargElementType?.let { remapType(it) }
        new.copyDynamicProperties(old)
    }

    private fun copyClass(old: IrClass): BirClass = copyReferencedElement(old, classes, {
        BirClassImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            origin = old.origin,
            visibility = old.visibility,
            name = old.name,
            isExternal = old.isExternal,
            kind = old.kind,
            modality = old.modality,
            isCompanion = old.isCompanion,
            isInner = old.isInner,
            isData = old.isData,
            isValue = old.isValue,
            isExpect = old.isExpect,
            isFun = old.isFun,
            hasEnumEntries = old.hasEnumEntries,
            source = old.source,
            superTypes = emptyList(),
            thisReceiver = null,
            valueClassRepresentation = null,
            signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
        )
    }) { new ->
        new.copyAttributes(old)
        new.thisReceiver = old.thisReceiver?.let { copyElement<BirValueParameter>(it) }
        new.typeParameters.copyElements(old.typeParameters)
        new.declarations.copyElements(old.declarations)
        new.superTypes = old.superTypes.memoryOptimizedMap { remapType(it) }
        new.valueClassRepresentation = old.valueClassRepresentation?.mapUnderlyingType { remapSimpleType(it) }
        new.copyDynamicProperties(old)
    }

    private fun copyAnonymousInitializer(old: IrAnonymousInitializer): BirAnonymousInitializer = copyNotReferencedElement(old) {
        val new = BirAnonymousInitializerImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            origin = old.origin,
            isStatic = old.isStatic,
            body = copyElement(old.body),
            signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
        )
        new.copyDynamicProperties(old)
        new
    }

    private fun copyTypeParameter(old: IrTypeParameter): BirTypeParameter = copyReferencedElement(old, typeParameters, {
        BirTypeParameterImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            origin = old.origin,
            name = old.name,
            variance = old.variance,
            isReified = old.isReified,
            superTypes = emptyList(),
            index = old.index,
            signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
        )
    }) { new ->
        new.superTypes = old.superTypes.memoryOptimizedMap { remapType(it) }
        new.copyDynamicProperties(old)
    }

    private fun copyConstructor(old: IrConstructor): BirConstructor = copyReferencedElement(old, constructors, {
        BirConstructorImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            origin = old.origin,
            visibility = old.visibility,
            name = old.name,
            isExternal = old.isExternal,
            isInline = old.isInline,
            isExpect = old.isExpect,
            dispatchReceiverParameter = null,
            extensionReceiverParameter = null,
            contextReceiverParametersCount = old.contextReceiverParametersCount,
            body = null,
            isPrimary = old.isPrimary,
            signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
        )
    }) { new ->
        new.dispatchReceiverParameter = old.dispatchReceiverParameter?.let { copyElement(it) }
        new.extensionReceiverParameter = old.extensionReceiverParameter?.let { copyElement(it) }
        new.valueParameters.copyElements(old.valueParameters)
        new.body = old.body?.let { copyElement(it) }
        new.typeParameters.copyElements(old.typeParameters)
        new.returnType = remapType(old.returnType)
        new.copyDynamicProperties(old)
    }

    private fun copyEnumEntry(old: IrEnumEntry): BirEnumEntry = copyReferencedElement(old, enumEntries, {
        BirEnumEntryImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            origin = old.origin,
            name = old.name,
            initializerExpression = null,
            correspondingClass = null,
            signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
        )
    }) { new ->
        new.initializerExpression = old.initializerExpression?.let { copyElement(it) }
        new.correspondingClass = old.correspondingClass?.let { copyElement(it) }
        new.copyDynamicProperties(old)
    }

    private fun copyErrorDeclaration(old: IrErrorDeclaration): BirErrorDeclaration = copyNotReferencedElement(old) {
        val new = BirErrorDeclarationImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            origin = old.origin,
            signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
        )
        new.copyDynamicProperties(old)
        new
    }

    private fun copyField(old: IrField): BirField = copyReferencedElement(old, fields, {
        BirFieldImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            origin = old.origin,
            visibility = old.visibility,
            name = old.name,
            isExternal = old.isExternal,
            type = BirUninitializedType,
            isFinal = old.isFinal,
            isStatic = old.isStatic,
            initializer = null,
            correspondingPropertySymbol = null,
            signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
        )
    }) { new ->
        new.initializer = old.initializer?.let { copyElement(it) }
        new.correspondingPropertySymbol = old.correspondingPropertySymbol?.let { remapSymbol(it) }
        new.type = remapType(old.type)
        new.copyDynamicProperties(old)
    }

    private fun copyLocalDelegatedProperty(old: IrLocalDelegatedProperty): BirLocalDelegatedProperty =
        copyReferencedElement(old, localDelegatedProperties, {
            BirLocalDelegatedPropertyImpl(
                sourceSpan = SourceSpan(old.startOffset, old.endOffset),
                origin = old.origin,
                name = old.name,
                type = BirUninitializedType,
                isVar = old.isVar,
                delegate = copyElement(old.delegate),
                getter = copyElement(old.getter),
                setter = null,
                signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
            )
        }) { new ->
            new.setter = old.setter?.let { copyElement(it) }
            new.type = remapType(old.type)
            new.copyDynamicProperties(old)
        }

    private fun copyModuleFragment(old: IrModuleFragment): BirModuleFragment = copyReferencedElement(old, modules, {
        BirModuleFragmentImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            descriptor = old.descriptor,
        )
    }) { new ->
        new.files.copyElements(old.files)
        new.copyDynamicProperties(old)
    }

    private fun copyProperty(old: IrProperty): BirProperty = copyReferencedElement(old, properties, {
        BirPropertyImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            origin = old.origin,
            name = old.name,
            isExternal = old.isExternal,
            visibility = old.visibility,
            modality = old.modality,
            isFakeOverride = old.isFakeOverride,
            overriddenSymbols = emptyList(),
            isVar = old.isVar,
            isConst = old.isConst,
            isLateinit = old.isLateinit,
            isDelegated = old.isDelegated,
            isExpect = old.isExpect,
            backingField = null,
            getter = null,
            setter = null,
            signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
        )
    }) { new ->
        new.copyAttributes(old)
        new.backingField = old.backingField?.let { copyElement(it) }
        new.getter = old.getter?.let { copyElement(it) }
        new.setter = old.setter?.let { copyElement(it) }
        new.overriddenSymbols = old.overriddenSymbols.memoryOptimizedMap { remapSymbol(it) }
        new.copyDynamicProperties(old)
    }

    private fun copyScript(old: IrScript): BirScript = copyReferencedElement(old, scripts, {
        BirScriptImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            origin = old.origin,
            name = old.name,
            thisReceiver = null,
            baseClass = null,
            providedProperties = old.providedProperties.memoryOptimizedMap { remapSymbol(it) },
            resultProperty = old.resultProperty?.let { remapSymbol(it) },
            earlierScriptsParameter = null,
            earlierScripts = old.earlierScripts?.memoryOptimizedMap { remapSymbol(it) },
            targetClass = old.targetClass?.let { remapSymbol(it) },
            constructor = null,
            importedScripts = old.importedScripts?.memoryOptimizedMap { remapSymbol(it) },
            signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
        )
    }) { new ->
        new.thisReceiver = old.thisReceiver?.let { copyElement(it) }
        new.explicitCallParameters.copyElements(old.explicitCallParameters)
        new.implicitReceiversParameters.copyElements(old.implicitReceiversParameters)
        new.providedPropertiesParameters.copyElements(old.providedPropertiesParameters)
        new.earlierScriptsParameter = old.earlierScriptsParameter?.let { copyElement(it) }
        new.constructor = old.constructor?.let { remapElement(it) }
        new.statements.copyElements(old.statements)
        new.baseClass = old.baseClass?.let { remapType(it) }
        new.copyDynamicProperties(old)
    }

    private fun copySimpleFunction(old: IrSimpleFunction): BirSimpleFunction = copyReferencedElement(old, functions, {
        BirSimpleFunctionImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            origin = old.origin,
            visibility = old.visibility,
            name = old.name,
            isExternal = old.isExternal,
            isInline = old.isInline,
            isExpect = old.isExpect,
            dispatchReceiverParameter = null,
            extensionReceiverParameter = null,
            contextReceiverParametersCount = old.contextReceiverParametersCount,
            body = null,
            modality = old.modality,
            isFakeOverride = old.isFakeOverride,
            overriddenSymbols = emptyList(),
            isTailrec = old.isTailrec,
            isSuspend = old.isSuspend,
            isOperator = old.isOperator,
            isInfix = old.isInfix,
            correspondingPropertySymbol = null,
            signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
        )
    }) { new ->
        new.copyAttributes(old)
        new.dispatchReceiverParameter = old.dispatchReceiverParameter?.let { copyElement(it) }
        new.extensionReceiverParameter = old.extensionReceiverParameter?.let { copyElement(it) }
        new.valueParameters.copyElements(old.valueParameters)
        new.body = old.body?.let { copyElement(it) }
        new.typeParameters.copyElements(old.typeParameters)
        new.overriddenSymbols = old.overriddenSymbols.memoryOptimizedMap { remapSymbol(it) }
        new.correspondingPropertySymbol = old.correspondingPropertySymbol?.let { remapSymbol(it) }
        new.returnType = remapType(old.returnType)
        new.copyDynamicProperties(old)
    }

    private fun copyTypeAlias(old: IrTypeAlias): BirTypeAlias = copyReferencedElement(old, typeAliases, {
        BirTypeAliasImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            origin = old.origin,
            name = old.name,
            visibility = old.visibility,
            isActual = old.isActual,
            expandedType = BirUninitializedType,
            signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
        )
    }) { new ->
        new.typeParameters.copyElements(old.typeParameters)
        new.expandedType = remapType(old.expandedType)
        new.copyDynamicProperties(old)
    }

    private fun copyVariable(old: IrVariable): BirVariable = copyReferencedElement(old, variables, {
        BirVariableImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            origin = old.origin,
            name = old.name,
            type = BirUninitializedType,
            isVar = old.isVar,
            isConst = old.isConst,
            isLateinit = old.isLateinit,
            initializer = null,
            signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
        )
    }) { new ->
        new.initializer = old.initializer?.let { copyElement(it) }
        new.type = remapType(old.type)
        new.copyDynamicProperties(old)
    }

    private fun copyExternalPackageFragment(old: IrExternalPackageFragment): BirExternalPackageFragment =
        copyReferencedElement(old, externalPackageFragments, {
            BirExternalPackageFragmentImpl(
                sourceSpan = SourceSpan(old.startOffset, old.endOffset),
                packageFqName = old.packageFqName,
                containerSource = if (old.symbol is DescriptorlessExternalPackageFragmentSymbol) null else old.containerSource,
                signature = if (old.symbol is DescriptorlessExternalPackageFragmentSymbol) null else old.symbol.signature,
            )
        }) { new ->
            new.declarations.copyElements(old.declarations)
            new.copyDynamicProperties(old)
        }

    private fun copyFile(old: IrFile): BirFile = copyReferencedElement(old, files, {
        BirFileImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            packageFqName = old.packageFqName,
            fileEntry = old.fileEntry,
            signature = old.symbol.signature,
            annotations = old.annotations.memoryOptimizedMap { remapElement(it) },
        )
    }) { new ->
        new.declarations.copyElements(old.declarations)
        new.copyDynamicProperties(old)
    }

    private fun copyExpressionBody(old: IrExpressionBody): BirExpressionBody = copyNotReferencedElement(old) {
        val new = BirExpressionBodyImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            expression = copyElement(old.expression),
        )
        new.copyDynamicProperties(old)
        new
    }

    private fun copyBlockBody(old: IrBlockBody): BirBlockBody = copyNotReferencedElement(old) {
        val new = BirBlockBodyImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
        )
        new.statements.copyElements(old.statements)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyConstructorCall(old: IrConstructorCall): BirConstructorCall = copyNotReferencedElement(old) {
        val new = BirConstructorCallImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            dispatchReceiver = old.dispatchReceiver?.let { copyElement(it) },
            extensionReceiver = old.extensionReceiver?.let { copyElement(it) },
            origin = old.origin,
            typeArguments = old.typeArguments.memoryOptimizedMap { it?.let { remapType(it) } },
            contextReceiversCount = old.contextReceiversCount,
            source = old.source,
            constructorTypeArgumentsCount = old.constructorTypeArgumentsCount,
        )
        new.copyAttributes(old)
        new.copyIrMemberAccessExpressionValueArguments(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyGetObjectValue(old: IrGetObjectValue): BirGetObjectValue = copyNotReferencedElement(old) {
        val new = BirGetObjectValueImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyGetEnumValue(old: IrGetEnumValue): BirGetEnumValue = copyNotReferencedElement(old) {
        val new = BirGetEnumValueImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyRawFunctionReference(old: IrRawFunctionReference): BirRawFunctionReference = copyNotReferencedElement(old) {
        val new = BirRawFunctionReferenceImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyBlock(old: IrBlock): BirBlock = copyNotReferencedElement(old) {
        val new = BirBlockImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            origin = old.origin,
        )
        new.copyAttributes(old)
        new.statements.copyElements(old.statements)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyComposite(old: IrComposite): BirComposite = copyNotReferencedElement(old) {
        val new = BirCompositeImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            origin = old.origin,
        )
        new.copyAttributes(old)
        new.statements.copyElements(old.statements)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyReturnableBlock(old: IrReturnableBlock): BirReturnableBlock = copyReferencedElement(old, returnableBlocks, {
        BirReturnableBlockImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            origin = old.origin,
            signature = old.symbol.signature,
        )
    }) { new ->
        new.copyAttributes(old)
        new.statements.copyElements(old.statements)
        new.copyDynamicProperties(old)
    }

    private fun copyInlinedFunctionBlock(old: IrInlinedFunctionBlock): BirInlinedFunctionBlock = copyNotReferencedElement(old) {
        val new = BirInlinedFunctionBlockImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            origin = old.origin,
            inlineCall = remapElement(old.inlineCall),
            inlinedElement = remapElement(old.inlinedElement),
        )
        new.copyAttributes(old)
        new.statements.copyElements(old.statements)
        new.copyDynamicProperties(old)
        new
    }

    private fun copySyntheticBody(old: IrSyntheticBody): BirSyntheticBody = copyNotReferencedElement(old) {
        val new = BirSyntheticBodyImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            kind = old.kind,
        )
        new.copyDynamicProperties(old)
        new
    }

    private fun copyBreak(old: IrBreak): BirBreak = copyNotReferencedElement(old) {
        val new = BirBreakImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            loop = remapElement(old.loop),
            label = old.label,
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyContinue(old: IrContinue): BirContinue = copyNotReferencedElement(old) {
        val new = BirContinueImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            loop = remapElement(old.loop),
            label = old.label,
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyCall(old: IrCall): BirCall = copyNotReferencedElement(old) {
        val new = BirCallImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            dispatchReceiver = old.dispatchReceiver?.let { copyElement(it) },
            extensionReceiver = old.extensionReceiver?.let { copyElement(it) },
            origin = old.origin,
            typeArguments = old.typeArguments.memoryOptimizedMap { it?.let { remapType(it) } },
            contextReceiversCount = old.contextReceiversCount,
            superQualifierSymbol = old.superQualifierSymbol?.let { remapSymbol(it) },
        )
        new.copyAttributes(old)
        new.copyIrMemberAccessExpressionValueArguments(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyFunctionReference(old: IrFunctionReference): BirFunctionReference = copyNotReferencedElement(old) {
        val new = BirFunctionReferenceImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            dispatchReceiver = old.dispatchReceiver?.let { copyElement(it) },
            extensionReceiver = old.extensionReceiver?.let { copyElement(it) },
            origin = old.origin,
            typeArguments = old.typeArguments.memoryOptimizedMap { it?.let { remapType(it) } },
            reflectionTarget = old.reflectionTarget?.let { remapSymbol(it) },
        )
        new.copyAttributes(old)
        new.copyIrMemberAccessExpressionValueArguments(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyPropertyReference(old: IrPropertyReference): BirPropertyReference = copyNotReferencedElement(old) {
        val new = BirPropertyReferenceImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            dispatchReceiver = old.dispatchReceiver?.let { copyElement(it) },
            extensionReceiver = old.extensionReceiver?.let { copyElement(it) },
            origin = old.origin,
            typeArguments = old.typeArguments.memoryOptimizedMap { it?.let { remapType(it) } },
            field = old.field?.let { remapSymbol(it) },
            getter = old.getter?.let { remapSymbol(it) },
            setter = old.setter?.let { remapSymbol(it) },
        )
        new.copyAttributes(old)
        new.copyIrMemberAccessExpressionValueArguments(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyLocalDelegatedPropertyReference(old: IrLocalDelegatedPropertyReference): BirLocalDelegatedPropertyReference =
        copyNotReferencedElement(old) {
            val new = BirLocalDelegatedPropertyReferenceImpl(
                sourceSpan = SourceSpan(old.startOffset, old.endOffset),
                type = remapType(old.type),
                symbol = remapSymbol(old.symbol),
                dispatchReceiver = old.dispatchReceiver?.let { copyElement(it) },
                extensionReceiver = old.extensionReceiver?.let { copyElement(it) },
                origin = old.origin,
                typeArguments = old.typeArguments.memoryOptimizedMap { it?.let { remapType(it) } },
                delegate = remapSymbol(old.delegate),
                getter = remapSymbol(old.getter),
                setter = old.setter?.let { remapSymbol(it) },
            )
            new.copyAttributes(old)
            new.copyIrMemberAccessExpressionValueArguments(old)
            new.copyDynamicProperties(old)
            new
        }

    private fun copyClassReference(old: IrClassReference): BirClassReference = copyNotReferencedElement(old) {
        val new = BirClassReferenceImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            classType = remapType(old.type),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun <T> copyConst(old: IrConst<T>): BirConst<T> = copyNotReferencedElement(old) {
        val new = BirConstImpl<T>(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            kind = old.kind,
            value = old.value,
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyConstantPrimitive(old: IrConstantPrimitive): BirConstantPrimitive = copyNotReferencedElement(old) {
        val new = BirConstantPrimitiveImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            value = copyElement(old.value),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyConstantObject(old: IrConstantObject): BirConstantObject = copyNotReferencedElement(old) {
        val new = BirConstantObjectImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            constructor = remapSymbol(old.constructor),
            typeArguments = old.typeArguments.memoryOptimizedMap { remapType(it) },
        )
        new.copyAttributes(old)
        new.valueArguments.copyElements(old.valueArguments)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyConstantArray(old: IrConstantArray): BirConstantArray = copyNotReferencedElement(old) {
        val new = BirConstantArrayImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
        )
        new.copyAttributes(old)
        new.elements.copyElements(old.elements)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyDelegatingConstructorCall(old: IrDelegatingConstructorCall): BirDelegatingConstructorCall =
        copyNotReferencedElement(old) {
            val new = BirDelegatingConstructorCallImpl(
                sourceSpan = SourceSpan(old.startOffset, old.endOffset),
                type = remapType(old.type),
                symbol = remapSymbol(old.symbol),
                dispatchReceiver = old.dispatchReceiver?.let { copyElement(it) },
                extensionReceiver = old.extensionReceiver?.let { copyElement(it) },
                origin = old.origin,
                typeArguments = old.typeArguments.memoryOptimizedMap { it?.let { remapType(it) } },
                contextReceiversCount = old.contextReceiversCount,
            )
            new.copyAttributes(old)
            new.copyIrMemberAccessExpressionValueArguments(old)
            new.copyDynamicProperties(old)
            new
        }

    private fun copyDynamicOperatorExpression(old: IrDynamicOperatorExpression): BirDynamicOperatorExpression =
        copyNotReferencedElement(old) {
            val new = BirDynamicOperatorExpressionImpl(
                sourceSpan = SourceSpan(old.startOffset, old.endOffset),
                type = remapType(old.type),
                operator = old.operator,
                receiver = copyElement(old.receiver),
            )
            new.copyAttributes(old)
            new.arguments.copyElements(old.arguments)
            new.copyDynamicProperties(old)
            new
        }

    private fun copyDynamicMemberExpression(old: IrDynamicMemberExpression): BirDynamicMemberExpression = copyNotReferencedElement(old) {
        val new = BirDynamicMemberExpressionImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            memberName = old.memberName,
            receiver = copyElement(old.receiver),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyEnumConstructorCall(old: IrEnumConstructorCall): BirEnumConstructorCall = copyNotReferencedElement(old) {
        val new = BirEnumConstructorCallImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            dispatchReceiver = old.dispatchReceiver?.let { copyElement(it) },
            extensionReceiver = old.extensionReceiver?.let { copyElement(it) },
            origin = old.origin,
            typeArguments = old.typeArguments.memoryOptimizedMap { it?.let { remapType(it) } },
            contextReceiversCount = old.contextReceiversCount,
        )
        new.copyAttributes(old)
        new.copyIrMemberAccessExpressionValueArguments(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyErrorCallExpression(old: IrErrorCallExpression): BirErrorCallExpression = copyNotReferencedElement(old) {
        val new = BirErrorCallExpressionImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            description = old.description,
            explicitReceiver = old.explicitReceiver?.let { copyElement(it) },
        )
        new.copyAttributes(old)
        new.arguments.copyElements(old.arguments)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyGetField(old: IrGetField): BirGetField = copyNotReferencedElement(old) {
        val new = BirGetFieldImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            superQualifierSymbol = old.superQualifierSymbol?.let { remapSymbol(it) },
            receiver = old.receiver?.let { copyElement(it) },
            origin = old.origin,
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copySetField(old: IrSetField): BirSetField = copyNotReferencedElement(old) {
        val new = BirSetFieldImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            superQualifierSymbol = old.superQualifierSymbol?.let { remapSymbol(it) },
            receiver = old.receiver?.let { copyElement(it) },
            origin = old.origin,
            value = copyElement(old.value),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyFunctionExpression(old: IrFunctionExpression): BirFunctionExpression = copyNotReferencedElement(old) {
        val new = BirFunctionExpressionImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            origin = old.origin,
            function = copyElement(old.function),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyGetClass(old: IrGetClass): BirGetClass = copyNotReferencedElement(old) {
        val new = BirGetClassImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            argument = copyElement(old.argument),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyInstanceInitializerCall(old: IrInstanceInitializerCall): BirInstanceInitializerCall = copyNotReferencedElement(old) {
        val new = BirInstanceInitializerCallImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            classSymbol = remapSymbol(old.classSymbol),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyWhileLoop(old: IrWhileLoop): BirWhileLoop = copyReferencedElement(old, loops, {
        BirWhileLoopImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            origin = old.origin,
            body = null,
            // nb: this may be a problem if there is a ref to the loop from within the condition (language seems to not allow that however).
            //  In such case the simples solution is to do what IR does right now - make condition property lateinit.
            condition = copyElement(old.condition),
            label = old.label,
        )
    }) { new ->
        new.copyAttributes(old)
        new.body = old.body?.let { copyElement(it) }
        new.copyDynamicProperties(old)
    }

    private fun copyDoWhileLoop(old: IrDoWhileLoop): BirDoWhileLoop = copyReferencedElement(old, loops, {
        BirDoWhileLoopImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            origin = old.origin,
            body = null,
            condition = copyElement(old.condition),
            label = old.label,
        )
    }) { new ->
        new.copyAttributes(old)
        new.body = old.body?.let { copyElement(it) }
        new.copyDynamicProperties(old)
    }

    private fun copyReturn(old: IrReturn): BirReturn = copyNotReferencedElement(old) {
        val new = BirReturnImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            value = copyElement(old.value),
            returnTargetSymbol = remapSymbol(old.returnTargetSymbol),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyStringConcatenation(old: IrStringConcatenation): BirStringConcatenation = copyNotReferencedElement(old) {
        val new = BirStringConcatenationImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
        )
        new.copyAttributes(old)
        new.arguments.copyElements(old.arguments)
        new.copyDynamicProperties(old)
        new
    }

    private fun copySuspensionPoint(old: IrSuspensionPoint): BirSuspensionPoint = copyNotReferencedElement(old) {
        val new = BirSuspensionPointImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            suspensionPointIdParameter = copyElement(old.suspensionPointIdParameter),
            result = copyElement(old.result),
            resumeResult = copyElement(old.resumeResult),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copySuspendableExpression(old: IrSuspendableExpression): BirSuspendableExpression = copyNotReferencedElement(old) {
        val new = BirSuspendableExpressionImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            suspensionPointId = copyElement(old.suspensionPointId),
            result = copyElement(old.result),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyThrow(old: IrThrow): BirThrow = copyNotReferencedElement(old) {
        val new = BirThrowImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            value = copyElement(old.value),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyTry(old: IrTry): BirTry = copyNotReferencedElement(old) {
        val new = BirTryImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            tryResult = copyElement(old.tryResult),
            finallyExpression = old.finallyExpression?.let { copyElement(it) },
        )
        new.copyAttributes(old)
        new.catches.copyElements(old.catches)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyCatch(old: IrCatch): BirCatch = copyNotReferencedElement(old) {
        val new = BirCatchImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            catchParameter = copyElement(old.catchParameter),
            result = copyElement(old.result),
        )
        new.copyDynamicProperties(old)
        new
    }

    private fun copyTypeOperatorCall(old: IrTypeOperatorCall): BirTypeOperatorCall = copyNotReferencedElement(old) {
        val new = BirTypeOperatorCallImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            operator = old.operator,
            argument = copyElement(old.argument),
            typeOperand = remapType(old.typeOperand),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyGetValue(old: IrGetValue): BirGetValue = copyNotReferencedElement(old) {
        val new = BirGetValueImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            origin = old.origin,
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copySetValue(old: IrSetValue): BirSetValue = copyNotReferencedElement(old) {
        val new = BirSetValueImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            origin = old.origin,
            value = copyElement(old.value),
        )
        new.copyAttributes(old)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyVararg(old: IrVararg): BirVararg = copyNotReferencedElement(old) {
        val new = BirVarargImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            varargElementType = remapType(old.varargElementType),
        )
        new.copyAttributes(old)
        new.elements.copyElements(old.elements)
        new.copyDynamicProperties(old)
        new
    }

    private fun copySpreadElement(old: IrSpreadElement): BirSpreadElement = copyNotReferencedElement(old) {
        val new = BirSpreadElementImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            expression = copyElement(old.expression),
        )
        new.copyDynamicProperties(old)
        new
    }

    private fun copyWhen(old: IrWhen): BirWhen = copyNotReferencedElement(old) {
        val new = BirWhenImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            origin = old.origin,
        )
        new.copyAttributes(old)
        new.branches.copyElements(old.branches)
        new.copyDynamicProperties(old)
        new
    }

    private fun copyBranch(old: IrBranch): BirBranch = copyNotReferencedElement(old) {
        val new = BirBranchImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            condition = copyElement(old.condition),
            result = copyElement(old.result),
        )
        new.copyDynamicProperties(old)
        new
    }

    private fun copyElseBranch(old: IrElseBranch): BirElseBranch = copyNotReferencedElement(old) {
        val new = BirElseBranchImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            condition = copyElement(old.condition),
            result = copyElement(old.result),
        )
        new.copyDynamicProperties(old)
        new
    }

    private fun copyErrorExpression(old: IrErrorExpression): BirErrorExpression = copyNotReferencedElement(old) {
        val new = BirErrorExpressionImpl(
            sourceSpan = SourceSpan(old.startOffset, old.endOffset),
            type = remapType(old.type),
            description = old.description,
        )
        new.copyDynamicProperties(old)
        new
    }


    private fun BirElement.copyDynamicProperties(from: IrElement) {
        this as BirElementBase

        if (from is IrExternalPackageFragment) {
            (this as BirExternalPackageFragment)[GlobalBirDynamicProperties.Descriptor] =
                if (from.symbol is DescriptorlessExternalPackageFragmentSymbol) null
                else mapDescriptor { from.packageFragmentDescriptor }
        } else if (from is IrDeclaration) {
            (this as BirDeclaration)[GlobalBirDynamicProperties.Descriptor] = mapDescriptor { from.descriptor }
        }

        if (from is IrMetadataSourceOwner) {
            (this as BirMetadataSourceOwner)[GlobalBirDynamicProperties.Metadata] = from.metadata
        }

        if (from is IrMemberWithContainerSource) {
            (this as BirMemberWithContainerSource)[GlobalBirDynamicProperties.ContainerSource] = from.containerSource
        }

        if (from is IrAttributeContainer) {
            (this as BirAttributeContainer)[GlobalBirDynamicProperties.OriginalBeforeInline] =
                from.originalBeforeInline?.let { remapElement(it) as BirAttributeContainer }
        }

        if (from is IrClass) {
            (this as BirClass)[GlobalBirDynamicProperties.SealedSubclasses] = from.sealedSubclasses.memoryOptimizedMap { remapSymbol(it) }
        }
    }

    override fun <Bir : BirElement> copyLazyElement(old: IrDeclaration): Bir? {
        @Suppress("UNCHECKED_CAST")
        return when (old) {
            is IrClass -> BirLazyClass(old, this)
            is IrSimpleFunction -> BirLazySimpleFunction(old, this)
            is IrConstructor -> BirLazyConstructor(old, this)
            is IrValueParameter -> BirLazyValueParameter(old, this)
            is IrProperty -> BirLazyProperty(old, this)
            is IrField -> BirLazyField(old, this)
            is IrEnumEntry -> BirLazyEnumEntry(old, this)
            is IrTypeAlias -> BirLazyTypeAlias(old, this)
            else -> null
        } as Bir?
    }
}
