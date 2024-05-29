/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementBase
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.declarations.impl.*
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.expressions.impl.*
import org.jetbrains.kotlin.bir.symbols.BirSymbol
import org.jetbrains.kotlin.bir.types.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.utils.mapOrTakeThisIfIdentity
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import java.util.*
import kotlin.collections.set

fun <E : BirElement> E.deepCopy(
    copier: BirTreeDeepCopier = BirTreeDeepCopier(),
): E {
    return copier.copyTree(this)
}

// todo: Could be adjusted for the change that all child fields are now nullable
// todo: Autogenerate
@OptIn(ObsoleteDescriptorBasedAPI::class)
@Suppress("NAME_SHADOWING")
open class BirTreeDeepCopier {
    protected var rootElement: BirElementBase? = null

    // explicit dispatch receiver to avoid capturing `this` in lambda
    private var lastDeferredInitialization: (BirTreeDeepCopier.(old: BirElement, new: BirElement) -> Unit)? = null
    private var lastDeferredInitializationOld: BirElement? = null
    private var lastDeferredInitializationNew: BirElement? = null

    protected val modules by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirModuleFragment>() }
    protected val classes by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirClass>() }
    protected val scripts by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirScript>() }
    protected val constructors by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirConstructor>() }
    protected val enumEntries by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirEnumEntry>() }
    protected val externalPackageFragments by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirExternalPackageFragment>() }
    protected val fields by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirField>() }
    protected val files by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirFile>() }
    protected val functions by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirSimpleFunction>() }
    protected val properties by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirProperty>() }
    protected val returnableBlocks by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirReturnableBlock>() }
    protected val typeParameters by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirTypeParameter>() }
    protected val valueParameters by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirValueParameter>() }
    protected val variables by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirVariable>() }
    protected val localDelegatedProperties by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirLocalDelegatedProperty>() }
    protected val typeAliases by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirTypeAlias>() }
    protected val loops by lazy(LazyThreadSafetyMode.NONE) { createElementMap<BirLoop>() }

    protected open fun <E : BirElement> createElementMap(): MutableMap<E, E> = IdentityHashMap<E, E>()


    fun <E : BirElement> copyElement(old: E): E {
        val new = copyElementPossiblyUnfinished(old)
        ensureLastElementIsFinished()
        return new
    }

    fun <E : BirElement?> copyElementPossiblyUnfinished(old: E): E {
        return doCopyElement(old, true)
    }

    protected fun <E : BirElement> deferInitialization(old: E, new: E, initialize: BirTreeDeepCopier.(old: E, new: E) -> Unit) {
        ensureLastElementIsFinished()
        @Suppress("UNCHECKED_CAST")
        lastDeferredInitialization = initialize as BirTreeDeepCopier.(BirElement, BirElement) -> Unit
        lastDeferredInitializationOld = old
        lastDeferredInitializationNew = new
    }

    fun ensureLastElementIsFinished() {
        while (true) {
            val last = lastDeferredInitialization ?: break
            lastDeferredInitialization = null
            last(lastDeferredInitializationOld!!, lastDeferredInitializationNew!!)
        }
    }

    protected open fun <ME : BirElement, SE : ME> copyReferencedElement(
        old: SE,
        map: MutableMap<ME, ME>,
        mustProduceNewCopy: Boolean, // to remove?
        copy: () -> SE,
    ): SE {
        // There is no computeIfAbsent, because:
        //  1. The copy() might modify the map.
        //     However, with careful use of deferInitialization it may be possible to avoid that.
        //  2. IdentityHashMap does not (yet) have an optimized implementation of computeIfAbsent,
        //     so there is no benefit using it.
        //     However, there may be a custom implementation which does this.
        //     Or, in case 1. is indeed relevant, it may return some hint object on failed get()
        //     to at least avoid hashing object twice.
        map[old]?.let {
            @Suppress("UNCHECKED_CAST")
            return it as SE
        }
        val new = copy()
        map[old] = new
        return new
    }

    protected fun <E : BirElement?> BirChildElementList<E>.copyElementsPossiblyUnfinished(from: BirChildElementList<E>) {
        for (element in from) {
            this += copyElementPossiblyUnfinished(element)
        }
    }

    protected open fun BirElement.copyAuxData(from: BirElement) {
        this as BirElementBase
        copyDynamicProperties(from as BirElementBase)
    }

    protected open fun BirAttributeContainer.copyAttributes(other: BirAttributeContainer) {
        attributeOwnerId = other.attributeOwnerId
        if (other.attributeOwnerId != other) {
            TODO("this[GlobalBirElementDynamicPropertyTokens.OriginalBeforeInline] = other.attributeOwnerId")
        }
    }

    fun <E : BirElement> remapElement(old: E): E {
        val new = remapElementPossiblyUnfinished(old)
        if (new !== old) {
            ensureLastElementIsFinished()
        }
        return new
    }

    fun <E : BirElement> remapElementPossiblyUnfinished(old: E): E {
        val rootElement = rootElement
        return if (rootElement == null || old === rootElement || rootElement.isAncestorOf(old as BirElementBase)) {
            doCopyElement(old, false)
        } else {
            old
        }
    }

    open fun <S : BirSymbol> remapSymbol(old: S): S {
        if (old is BirElementBase) {
            return remapElement(old)
        } else {
            return old
        }
    }

    // unlike the impl at org.jetbrains.kotlin.ir.util.DeepCopyTypeRemapper,
    //  this also remaps classes of types other than BirSimpleType
    // todo: check what's bout `annotations` - esp. how they can be copied/reused
    open fun remapType(old: BirType): BirType = when (old) {
        is BirSimpleType -> remapSimpleType(old)
        is BirDynamicType -> old
        is BirErrorType -> old
        else -> TODO(old.toString())
    }

    protected open fun remapSimpleType(old: BirSimpleType): BirSimpleType {
        val classifier = remapSymbol(old.classifier)
        val arguments = old.arguments.mapOrTakeThisIfIdentity { remapTypeArgument(it) }
        val abbreviation = old.abbreviation?.let { remapTypeAbbreviation(it) }

        return if (classifier === old.classifier && arguments === old.arguments && abbreviation === old.abbreviation) {
            old
        } else {
            BirSimpleTypeImpl(
                old.kotlinType,
                classifier,
                old.nullability,
                arguments,
                old.annotations.memoryOptimizedMap { copyElement(it) },
                abbreviation,
            )
        }
    }

    open fun remapTypeArgument(old: BirTypeArgument): BirTypeArgument = when (old) {
        is BirStarProjection -> old
        is BirType -> remapType(old) as BirTypeArgument
        is BirTypeProjection -> {
            val newType = remapType(old.type)
            if (newType === old.type) {
                old
            } else {
                BirTypeProjectionImpl(newType, old.variance)
            }
        }
    }

    open fun remapTypeAbbreviation(old: BirTypeAbbreviation): BirTypeAbbreviation {
        val typeAlias = remapSymbol(old.typeAlias)
        val arguments = old.arguments.mapOrTakeThisIfIdentity { remapTypeArgument(it) }

        return if (typeAlias === old.typeAlias && arguments === old.arguments) {
            old
        } else {
            BirTypeAbbreviation(
                typeAlias,
                old.hasQuestionMark,
                arguments,
                old.annotations.memoryOptimizedMap { copyElement(it) },
            )
        }
    }

    fun <T : BirElement> copyTree(root: T): T {
        require(rootElement == null) { "Trying to recursively copy a tree" }
        rootElement = root as BirElementBase
        val new = copyElement(root)
        rootElement = null
        return new
    }


    @Suppress("UNCHECKED_CAST")
    protected open fun <E : BirElement?> doCopyElement(old: E, mustProduceNewCopy: Boolean): E = when (old) {
        null -> null
        is BirValueParameter -> copyValueParameter(old, mustProduceNewCopy)
        is BirClass -> copyClass(old, mustProduceNewCopy)
        is BirAnonymousInitializer -> copyAnonymousInitializer(old)
        is BirTypeParameter -> copyTypeParameter(old, mustProduceNewCopy)
        is BirConstructor -> copyConstructor(old, mustProduceNewCopy)
        is BirEnumEntry -> copyEnumEntry(old, mustProduceNewCopy)
        is BirErrorDeclaration -> copyErrorDeclaration(old)
        is BirField -> copyField(old, mustProduceNewCopy)
        is BirLocalDelegatedProperty -> copyLocalDelegatedProperty(old, mustProduceNewCopy)
        is BirModuleFragment -> copyModuleFragment(old, mustProduceNewCopy)
        is BirProperty -> copyProperty(old, mustProduceNewCopy)
        is BirScript -> copyScript(old, mustProduceNewCopy)
        is BirSimpleFunction -> copySimpleFunction(old, mustProduceNewCopy)
        is BirTypeAlias -> copyTypeAlias(old, mustProduceNewCopy)
        is BirVariable -> copyVariable(old, mustProduceNewCopy)
        is BirExternalPackageFragment -> copyExternalPackageFragment(old, mustProduceNewCopy)
        is BirFile -> copyFile(old, mustProduceNewCopy)
        is BirExpressionBody -> copyExpressionBody(old)
        is BirBlockBody -> copyBlockBody(old)
        is BirConstructorCall -> copyConstructorCall(old)
        is BirGetObjectValue -> copyGetObjectValue(old)
        is BirGetEnumValue -> copyGetEnumValue(old)
        is BirRawFunctionReference -> copyRawFunctionReference(old)
        is BirComposite -> copyComposite(old)
        is BirReturnableBlock -> copyReturnableBlock(old, mustProduceNewCopy)
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
        is BirWhileLoop -> copyWhileLoop(old, mustProduceNewCopy)
        is BirDoWhileLoop -> copyDoWhileLoop(old, mustProduceNewCopy)
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
        else -> error(old)
    } as E

    open fun copyValueParameter(old: BirValueParameter, mustProduceNewCopy: Boolean): BirElement =
        copyReferencedElement(old, valueParameters, mustProduceNewCopy) {
            val new = BirValueParameterImpl(
                sourceSpan = old.sourceSpan,
                origin = old.origin,
                name = old.name,
                type = remapType(old.type),
                isAssignable = old.isAssignable,
                varargElementType = null,
                isCrossinline = old.isCrossinline,
                isNoinline = old.isNoinline,
                isHidden = old.isHidden,
                defaultValue = null,
                signature = old.signature,
                index = old.index,
                annotations = old.annotations.map { remapElement(it) },
            )
            new.copyAuxData(old)
            deferInitialization(old, new) { old, new ->
                new.defaultValue = old.defaultValue?.let { copyElementPossiblyUnfinished(it) }
                new.varargElementType = old.varargElementType?.let { remapType(it) }
            }
            new
        }

    open fun copyClass(old: BirClass, mustProduceNewCopy: Boolean): BirElement = copyReferencedElement(old, classes, mustProduceNewCopy) {
        val new = BirClassImpl(
            sourceSpan = old.sourceSpan,
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
            signature = old.signature,
            annotations = old.annotations.map { remapElement(it) },
        )
        new.copyAuxData(old)
        deferInitialization(old, new) { old, new ->
            new.copyAttributes(old)
            new.thisReceiver = old.thisReceiver?.let { copyElementPossiblyUnfinished(it) }
            new.typeParameters.copyElementsPossiblyUnfinished(old.typeParameters)
            new.declarations.copyElementsPossiblyUnfinished(old.declarations)
            new.superTypes = old.superTypes.memoryOptimizedMap { remapType(it) }
            new.valueClassRepresentation = old.valueClassRepresentation?.mapUnderlyingType { remapType(it) as BirSimpleType }
        }
        new
    }

    open fun copyAnonymousInitializer(old: BirAnonymousInitializer): BirElement {
        val new = BirAnonymousInitializerImpl(
            sourceSpan = old.sourceSpan,
            origin = old.origin,
            isStatic = old.isStatic,
            body = copyElementPossiblyUnfinished(old.body),
            signature = old.signature,
            annotations = old.annotations.map { remapElement(it) },
        )
        new.copyAuxData(old)
        return new
    }

    open fun copyTypeParameter(old: BirTypeParameter, mustProduceNewCopy: Boolean): BirElement =
        copyReferencedElement(old, typeParameters, mustProduceNewCopy) {
            val new = BirTypeParameterImpl(
                sourceSpan = old.sourceSpan,
                origin = old.origin,
                name = old.name,
                variance = old.variance,
                isReified = old.isReified,
                superTypes = emptyList(),
                index = old.index,
                signature = old.signature,
                annotations = old.annotations.map { remapElement(it) },
            )
            new.copyAuxData(old)
            deferInitialization(old, new) { old, new ->
                new.superTypes = old.superTypes.memoryOptimizedMap { remapType(it) }
            }
            new
        }

    open fun copyConstructor(old: BirConstructor, mustProduceNewCopy: Boolean): BirElement =
        copyReferencedElement(old, constructors, mustProduceNewCopy) {
            val new = BirConstructorImpl(
                sourceSpan = old.sourceSpan,
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
                signature = old.signature,
                annotations = old.annotations.map { remapElement(it) },
            )

            new.copyAuxData(old)
            deferInitialization(old, new) { old, new ->
                new.dispatchReceiverParameter = old.dispatchReceiverParameter?.let { copyElementPossiblyUnfinished(it) }
                new.extensionReceiverParameter = old.extensionReceiverParameter?.let { copyElementPossiblyUnfinished(it) }
                new.valueParameters.copyElementsPossiblyUnfinished(old.valueParameters)
                new.body = old.body?.let { copyElementPossiblyUnfinished(it) }
                new.typeParameters.copyElementsPossiblyUnfinished(old.typeParameters)
                new.returnType = remapType(old.returnType)
            }
            new
        }

    open fun copyEnumEntry(old: BirEnumEntry, mustProduceNewCopy: Boolean): BirElement =
        copyReferencedElement(old, enumEntries, mustProduceNewCopy) {
            val new = BirEnumEntryImpl(
                sourceSpan = old.sourceSpan,
                origin = old.origin,
                name = old.name,
                signature = old.signature,
                initializerExpression = null,
                correspondingClass = null,
                annotations = old.annotations.map { remapElement(it) },
            )
            new.copyAuxData(old)
            deferInitialization(old, new) { old, new ->
                new.initializerExpression = old.initializerExpression?.let { copyElementPossiblyUnfinished(it) }
                new.correspondingClass = old.correspondingClass?.let { copyElementPossiblyUnfinished(it) }
            }
            new
        }

    open fun copyErrorDeclaration(old: BirErrorDeclaration): BirElement {
        val new = BirErrorDeclarationImpl(
            sourceSpan = old.sourceSpan,
            origin = old.origin,
            signature = old.signature,
            annotations = old.annotations.map { remapElement(it) },
        )
        new.copyAuxData(old)
        return new
    }

    open fun copyField(old: BirField, mustProduceNewCopy: Boolean): BirElement = copyReferencedElement(old, fields, mustProduceNewCopy) {
        val new = BirFieldImpl(
            sourceSpan = old.sourceSpan,
            origin = old.origin,
            visibility = old.visibility,
            name = old.name,
            isExternal = old.isExternal,
            type = remapType(old.type),
            isFinal = old.isFinal,
            isStatic = old.isStatic,
            initializer = null,
            correspondingPropertySymbol = null,
            signature = old.signature,
            annotations = old.annotations.map { remapElement(it) },
        )
        new.copyAuxData(old)
        deferInitialization(old, new) { old, new ->
            new.initializer = old.initializer?.let { copyElementPossiblyUnfinished(it) }
            new.correspondingPropertySymbol = old.correspondingPropertySymbol?.let { remapSymbol(it) }
        }
        new
    }

    open fun copyLocalDelegatedProperty(old: BirLocalDelegatedProperty, mustProduceNewCopy: Boolean): BirElement =
        copyReferencedElement(old, localDelegatedProperties, mustProduceNewCopy) {
            val new = BirLocalDelegatedPropertyImpl(
                sourceSpan = old.sourceSpan,
                origin = old.origin,
                name = old.name,
                type = remapType(old.type),
                isVar = old.isVar,
                delegate = copyElementPossiblyUnfinished(old.delegate),
                getter = copyElementPossiblyUnfinished(old.getter),
                setter = null,
                signature = old.signature,
                annotations = old.annotations.map { remapElement(it) },
            )
            new.copyAuxData(old)
            deferInitialization(old, new) { old, new ->
                new.setter = old.setter?.let { copyElementPossiblyUnfinished(it) }
            }

            new
        }

    open fun copyModuleFragment(old: BirModuleFragment, mustProduceNewCopy: Boolean): BirElement =
        copyReferencedElement(old, modules, mustProduceNewCopy) {
            val new = BirModuleFragmentImpl(
                sourceSpan = old.sourceSpan,
                descriptor = old.descriptor,
            )
            new.copyAuxData(old)
            deferInitialization(old, new) { old, new ->
                new.files.copyElementsPossiblyUnfinished(old.files)
            }
            new
        }

    open fun copyProperty(old: BirProperty, mustProduceNewCopy: Boolean): BirElement =
        copyReferencedElement(old, properties, mustProduceNewCopy) {
            val new = BirPropertyImpl(
                sourceSpan = old.sourceSpan,
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
                signature = old.signature,
                annotations = old.annotations.map { remapElement(it) },
            )
            new.copyAuxData(old)
            deferInitialization(old, new) { old, new ->
                new.copyAttributes(old)
                new.backingField = old.backingField?.let { copyElementPossiblyUnfinished(it) }
                new.getter = old.getter?.let { copyElementPossiblyUnfinished(it) }
                new.setter = old.setter?.let { copyElementPossiblyUnfinished(it) }
                new.overriddenSymbols = old.overriddenSymbols.memoryOptimizedMap { remapSymbol(it) }
            }
            new
        }

    open fun copyScript(old: BirScript, mustProduceNewCopy: Boolean): BirElement = copyReferencedElement(old, scripts, mustProduceNewCopy) {
        val new = BirScriptImpl(
            sourceSpan = old.sourceSpan,
            origin = old.origin,
            name = old.name,
            thisReceiver = null,
            baseClass = null,
            providedProperties = emptyList(),
            resultProperty = null,
            earlierScriptsParameter = null,
            earlierScripts = null,
            targetClass = null,
            constructor = null,
            signature = old.signature,
            importedScripts = old.importedScripts,
            annotations = old.annotations.map { remapElement(it) },
        )
        new.copyAuxData(old)
        deferInitialization(old, new) { old, new ->
            new.copyAuxData(old)
            new.thisReceiver = old.thisReceiver?.let { copyElementPossiblyUnfinished(it) }
            new.explicitCallParameters.copyElementsPossiblyUnfinished(old.explicitCallParameters)
            new.implicitReceiversParameters.copyElementsPossiblyUnfinished(old.implicitReceiversParameters)
            new.providedProperties = old.providedProperties.memoryOptimizedMap { remapSymbol(it) }
            new.providedPropertiesParameters.copyElementsPossiblyUnfinished(old.providedPropertiesParameters)
            new.resultProperty = old.resultProperty?.let { remapSymbol(it) }
            new.earlierScriptsParameter = old.earlierScriptsParameter?.let { copyElementPossiblyUnfinished(it) }
            new.earlierScripts = old.earlierScripts?.memoryOptimizedMap { remapSymbol(it) }
            new.targetClass = old.targetClass?.let { remapSymbol(it) }
            new.constructor = old.constructor?.let { remapElementPossiblyUnfinished(it) }
            new.statements.copyElementsPossiblyUnfinished(old.statements)
            new.baseClass = old.baseClass?.let { remapType(it) }
        }
        new
    }

    open fun copySimpleFunction(old: BirSimpleFunction, mustProduceNewCopy: Boolean): BirElement =
        copyReferencedElement(old, functions, mustProduceNewCopy) {
            val new = BirSimpleFunctionImpl(
                sourceSpan = old.sourceSpan,
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
                signature = old.signature,
                annotations = old.annotations.map { remapElement(it) },
            )
            new.copyAuxData(old)
            deferInitialization(old, new) { old, new ->
                new.copyAttributes(old)
                new.dispatchReceiverParameter = old.dispatchReceiverParameter?.let { copyElementPossiblyUnfinished(it) }
                new.extensionReceiverParameter = old.extensionReceiverParameter?.let { copyElementPossiblyUnfinished(it) }
                new.valueParameters.copyElementsPossiblyUnfinished(old.valueParameters)
                new.body = old.body?.let { copyElementPossiblyUnfinished(it) }
                new.typeParameters.copyElementsPossiblyUnfinished(old.typeParameters)
                new.overriddenSymbols = old.overriddenSymbols.memoryOptimizedMap { remapSymbol(it) }
                new.correspondingPropertySymbol = old.correspondingPropertySymbol?.let { remapSymbol(it) }
                new.returnType = remapType(old.returnType)
            }
            new
        }

    open fun copyTypeAlias(old: BirTypeAlias, mustProduceNewCopy: Boolean): BirElement =
        copyReferencedElement(old, typeAliases, mustProduceNewCopy) {
            val new = BirTypeAliasImpl(
                sourceSpan = old.sourceSpan,
                origin = old.origin,
                name = old.name,
                visibility = old.visibility,
                isActual = old.isActual,
                expandedType = remapType(old.expandedType),
                signature = old.signature,
                annotations = old.annotations.map { remapElement(it) },
            )
            new.copyAuxData(old)
            deferInitialization(old, new) { old, new ->
                new.typeParameters.copyElementsPossiblyUnfinished(old.typeParameters)
            }
            new
        }

    open fun copyVariable(old: BirVariable, mustProduceNewCopy: Boolean): BirElement =
        copyReferencedElement(old, variables, mustProduceNewCopy) {
            val new = BirVariableImpl(
                sourceSpan = old.sourceSpan,
                origin = old.origin,
                name = old.name,
                type = remapType(old.type),
                isVar = old.isVar,
                isConst = old.isConst,
                isLateinit = old.isLateinit,
                initializer = null,
                signature = old.signature,
                annotations = old.annotations.map { remapElement(it) },
            )
            new.copyAuxData(old)
            deferInitialization(old, new) { old, new ->
                new.initializer = old.initializer?.let { copyElementPossiblyUnfinished(it) }
            }
            new
        }

    open fun copyExternalPackageFragment(old: BirExternalPackageFragment, mustProduceNewCopy: Boolean): BirElement =
        copyReferencedElement(old, externalPackageFragments, mustProduceNewCopy) {
            val new = BirExternalPackageFragmentImpl(
                sourceSpan = old.sourceSpan,
                packageFqName = old.packageFqName,
                containerSource = old.containerSource,
                signature = old.signature,
            )
            new.copyAuxData(old)
            deferInitialization(old, new) { old, new ->
                new.declarations.copyElementsPossiblyUnfinished(old.declarations)
            }

            new
        }

    open fun copyFile(old: BirFile, mustProduceNewCopy: Boolean): BirElement = copyReferencedElement(old, files, mustProduceNewCopy) {
        val new = BirFileImpl(
            sourceSpan = old.sourceSpan,
            packageFqName = old.packageFqName,
            fileEntry = old.fileEntry,
            signature = old.signature,
            annotations = old.annotations.map { remapElement(it) },
        )
        new.copyAuxData(old)
        deferInitialization(old, new) { old, new ->
            new.declarations.copyElementsPossiblyUnfinished(old.declarations)
        }
        new
    }

    open fun copyExpressionBody(old: BirExpressionBody): BirElement {
        val new = BirExpressionBodyImpl(
            sourceSpan = old.sourceSpan,
            expression = copyElementPossiblyUnfinished(old.expression),
        )
        new.copyAuxData(old)
        return new
    }

    open fun copyBlockBody(old: BirBlockBody): BirElement {
        val new = BirBlockBodyImpl(
            sourceSpan = old.sourceSpan,
        )
        new.copyAuxData(old)
        deferInitialization(old, new) { old, new ->
            new.statements.copyElementsPossiblyUnfinished(old.statements)
        }
        return new
    }

    open fun copyConstructorCall(old: BirConstructorCall): BirElement {
        val new = BirConstructorCallImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            dispatchReceiver = null,
            extensionReceiver = null,
            origin = old.origin,
            typeArguments = old.typeArguments.memoryOptimizedMap { it?.let { remapType(it) } },
            contextReceiversCount = old.contextReceiversCount,
            source = old.source,
            constructorTypeArgumentsCount = old.constructorTypeArgumentsCount,
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.dispatchReceiver =
                old.dispatchReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
            new.extensionReceiver =
                old.extensionReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
            new.valueArguments.copyElementsPossiblyUnfinished(old.valueArguments)
        }
        return new
    }

    open fun copyGetObjectValue(old: BirGetObjectValue): BirElement {
        val new = BirGetObjectValueImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyGetEnumValue(old: BirGetEnumValue): BirElement {
        val new = BirGetEnumValueImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyRawFunctionReference(old: BirRawFunctionReference): BirElement {
        val new = BirRawFunctionReferenceImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyBlock(old: BirBlock): BirElement {
        val new = BirBlockImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            origin = old.origin,
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.statements.copyElementsPossiblyUnfinished(old.statements)
        }
        return new
    }

    open fun copyComposite(old: BirComposite): BirElement {
        val new = BirCompositeImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            origin = old.origin,
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.statements.copyElementsPossiblyUnfinished(old.statements)
        }
        return new
    }

    open fun copyReturnableBlock(old: BirReturnableBlock, mustProduceNewCopy: Boolean): BirElement =
        copyReferencedElement(old, returnableBlocks, mustProduceNewCopy) {
            val new = BirReturnableBlockImpl(
                sourceSpan = old.sourceSpan,
                type = remapType(old.type),
                origin = old.origin,
                signature = old.signature,
            )
            new.copyAuxData(old)
            deferInitialization(old, new) { old, new ->
                new.copyAttributes(old)
                new.statements.copyElementsPossiblyUnfinished(old.statements)
            }
            new
        }

    open fun copyInlinedFunctionBlock(old: BirInlinedFunctionBlock): BirElement { // no remap
        // no remap
        val new = BirInlinedFunctionBlockImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            origin = old.origin,
            inlineCall = old.inlineCall, // no remap
            inlinedElement = old.inlinedElement, // no remap
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.statements.copyElementsPossiblyUnfinished(old.statements)
        }
        // no remap
        // no remap
        return new
    }

    open fun copySyntheticBody(old: BirSyntheticBody): BirElement {
        val new = BirSyntheticBodyImpl(
            sourceSpan = old.sourceSpan,
            kind = old.kind,
        )
        new.copyAuxData(old)
        return new
    }

    open fun copyBreak(old: BirBreak): BirElement {
        val new = BirBreakImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            loop = remapElementPossiblyUnfinished(old.loop),
            label = old.label,
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyContinue(old: BirContinue): BirElement {
        val new = BirContinueImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            loop = remapElementPossiblyUnfinished(old.loop),
            label = old.label,
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyCall(old: BirCall): BirElement {
        val new = BirCallImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            dispatchReceiver = null,
            extensionReceiver = null,
            origin = old.origin,
            typeArguments = old.typeArguments.memoryOptimizedMap { it?.let { remapType(it) } },
            contextReceiversCount = old.contextReceiversCount,
            superQualifierSymbol = old.superQualifierSymbol?.let { remapSymbol(it) },
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.dispatchReceiver =
                old.dispatchReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
            new.extensionReceiver =
                old.extensionReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
            new.valueArguments.copyElementsPossiblyUnfinished(old.valueArguments)
        }
        return new
    }

    open fun copyFunctionReference(old: BirFunctionReference): BirElement {
        val new = BirFunctionReferenceImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            dispatchReceiver = null,
            extensionReceiver = null,
            origin = old.origin,
            typeArguments = old.typeArguments.memoryOptimizedMap { it?.let { remapType(it) } },
            reflectionTarget = old.reflectionTarget?.let { remapSymbol(it) },
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.dispatchReceiver =
                old.dispatchReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
            new.extensionReceiver =
                old.extensionReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
            new.valueArguments.copyElementsPossiblyUnfinished(old.valueArguments)
        }
        return new
    }

    open fun copyPropertyReference(old: BirPropertyReference): BirElement {
        val new = BirPropertyReferenceImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            dispatchReceiver = null,
            extensionReceiver = null,
            origin = old.origin,
            typeArguments = old.typeArguments.memoryOptimizedMap { it?.let { remapType(it) } },
            field = old.field?.let { remapSymbol(it) },
            getter = old.getter?.let { remapSymbol(it) },
            setter = old.setter?.let { remapSymbol(it) },
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.dispatchReceiver =
                old.dispatchReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
            new.extensionReceiver =
                old.extensionReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
            new.valueArguments.copyElementsPossiblyUnfinished(old.valueArguments)
        }
        return new
    }

    open fun copyLocalDelegatedPropertyReference(old: BirLocalDelegatedPropertyReference): BirElement {
        val new = BirLocalDelegatedPropertyReferenceImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            dispatchReceiver = null,
            extensionReceiver = null,
            origin = old.origin,
            typeArguments = old.typeArguments.memoryOptimizedMap { it?.let { remapType(it) } },
            delegate = remapSymbol(old.delegate),
            getter = remapSymbol(old.getter),
            setter = old.setter?.let { remapSymbol(it) },
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.dispatchReceiver =
                old.dispatchReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
            new.extensionReceiver =
                old.extensionReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
            new.valueArguments.copyElementsPossiblyUnfinished(old.valueArguments)
        }
        return new
    }

    open fun copyClassReference(old: BirClassReference): BirElement {
        val new = BirClassReferenceImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            classType = remapType(old.type),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun <T> copyConst(old: BirConst<T>): BirConst<T> {
        val new = BirConstImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            kind = old.kind,
            value = old.value,
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyConstantPrimitive(old: BirConstantPrimitive): BirElement {
        val new = BirConstantPrimitiveImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            value = copyElementPossiblyUnfinished(old.value),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyConstantObject(old: BirConstantObject): BirElement {
        val new = BirConstantObjectImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            constructor = remapSymbol(old.constructor),
            typeArguments = old.typeArguments.memoryOptimizedMap { remapType(it) },
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.valueArguments.copyElementsPossiblyUnfinished(old.valueArguments)
        }
        return new
    }

    open fun copyConstantArray(old: BirConstantArray): BirElement {
        val new = BirConstantArrayImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.elements.copyElementsPossiblyUnfinished(old.elements)
        }
        return new
    }

    open fun copyDelegatingConstructorCall(old: BirDelegatingConstructorCall): BirElement {
        val new = BirDelegatingConstructorCallImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            dispatchReceiver = null,
            extensionReceiver = null,
            origin = old.origin,
            typeArguments = old.typeArguments.memoryOptimizedMap { it?.let { remapType(it) } },
            contextReceiversCount = old.contextReceiversCount,
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.dispatchReceiver =
                old.dispatchReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
            new.extensionReceiver =
                old.extensionReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
            new.valueArguments.copyElementsPossiblyUnfinished(old.valueArguments)
        }
        return new
    }

    open fun copyDynamicOperatorExpression(old: BirDynamicOperatorExpression): BirElement {
        val new = BirDynamicOperatorExpressionImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            operator = old.operator,
            receiver = copyElementPossiblyUnfinished(old.receiver),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.arguments.copyElementsPossiblyUnfinished(old.arguments)
        }
        return new
    }

    open fun copyDynamicMemberExpression(old: BirDynamicMemberExpression): BirElement {
        val new = BirDynamicMemberExpressionImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            memberName = old.memberName,
            receiver = copyElementPossiblyUnfinished(old.receiver),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyEnumConstructorCall(old: BirEnumConstructorCall): BirElement {
        val new = BirEnumConstructorCallImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            dispatchReceiver = null,
            extensionReceiver = null,
            origin = old.origin,
            typeArguments = old.typeArguments.memoryOptimizedMap { it?.let { remapType(it) } },
            contextReceiversCount = old.contextReceiversCount,
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.dispatchReceiver =
                old.dispatchReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
            new.extensionReceiver =
                old.extensionReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
            new.valueArguments.copyElementsPossiblyUnfinished(old.valueArguments)
        }
        return new
    }

    open fun copyErrorCallExpression(old: BirErrorCallExpression): BirElement {
        val new = BirErrorCallExpressionImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            description = old.description,
            explicitReceiver = null,
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.arguments.copyElementsPossiblyUnfinished(old.arguments)
            new.explicitReceiver =
                old.explicitReceiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
        }
        return new
    }

    open fun copyGetField(old: BirGetField): BirElement {
        val new = BirGetFieldImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            superQualifierSymbol = old.superQualifierSymbol?.let { remapSymbol(it) },
            receiver = null,
            origin = old.origin,
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.receiver =
                old.receiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
        }
        return new
    }

    open fun copySetField(old: BirSetField): BirElement {
        val new = BirSetFieldImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            superQualifierSymbol = old.superQualifierSymbol?.let { remapSymbol(it) },
            receiver = null,
            origin = old.origin,
            value = copyElementPossiblyUnfinished(old.value),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.receiver =
                old.receiver?.let {
                    copyElementPossiblyUnfinished(it)
                }
        }
        return new
    }

    open fun copyFunctionExpression(old: BirFunctionExpression): BirElement {
        val new = BirFunctionExpressionImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            origin = old.origin,
            function = copyElementPossiblyUnfinished(old.function),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyGetClass(old: BirGetClass): BirElement {
        val new = BirGetClassImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            argument = copyElementPossiblyUnfinished(old.argument),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyInstanceInitializerCall(old: BirInstanceInitializerCall): BirElement {
        val new = BirInstanceInitializerCallImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            classSymbol = remapSymbol(old.classSymbol),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyWhileLoop(old: BirWhileLoop, mustProduceNewCopy: Boolean): BirElement =
        copyReferencedElement(old, loops, mustProduceNewCopy) {
            val new = BirWhileLoopImpl(
                sourceSpan = old.sourceSpan,
                type = remapType(old.type),
                origin = old.origin,
                body = null,
                // nb: this may be a problem if there is a ref to the loop from within the condition (language seems to not allow that however).
                //  In such case the simples solution is to do what IR does right now - make condition property lateinit.
                condition = copyElementPossiblyUnfinished(old.condition),
                label = old.label,
            )
            new.copyAuxData(old)
            deferInitialization(old, new) { old, new ->
                new.copyAttributes(old)
                new.body = old.body?.let { copyElementPossiblyUnfinished(it) }
            }
            new
        }

    open fun copyDoWhileLoop(old: BirDoWhileLoop, mustProduceNewCopy: Boolean): BirElement =
        copyReferencedElement(old, loops, mustProduceNewCopy) {
            val new = BirDoWhileLoopImpl(
                sourceSpan = old.sourceSpan,
                type = remapType(old.type),
                origin = old.origin,
                body = null,
                condition = copyElementPossiblyUnfinished(old.condition),
                label = old.label,
            )
            new.copyAuxData(old)
            deferInitialization(old, new) { old, new ->
                new.copyAttributes(old)
                new.body = old.body?.let { copyElementPossiblyUnfinished(it) }
            }
            new
        }

    open fun copyReturn(old: BirReturn): BirElement {
        val new = BirReturnImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            value = copyElementPossiblyUnfinished(old.value),
            returnTargetSymbol = remapSymbol(old.returnTargetSymbol),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyStringConcatenation(old: BirStringConcatenation): BirElement {
        val new = BirStringConcatenationImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.arguments.copyElementsPossiblyUnfinished(old.arguments)
        }
        return new
    }

    open fun copySuspensionPoint(old: BirSuspensionPoint): BirElement {
        val new = BirSuspensionPointImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            suspensionPointIdParameter = copyElementPossiblyUnfinished(old.suspensionPointIdParameter),
            result = copyElementPossiblyUnfinished(old.result),
            resumeResult = copyElementPossiblyUnfinished(old.resumeResult),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copySuspendableExpression(old: BirSuspendableExpression): BirElement {
        val new = BirSuspendableExpressionImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            suspensionPointId = copyElementPossiblyUnfinished(old.suspensionPointId),
            result = copyElementPossiblyUnfinished(old.result),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyThrow(old: BirThrow): BirElement {
        val new = BirThrowImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            value = copyElementPossiblyUnfinished(old.value),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyTry(old: BirTry): BirElement {
        val new = BirTryImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            tryResult = copyElementPossiblyUnfinished(old.tryResult),
            finallyExpression = null,
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.catches.copyElementsPossiblyUnfinished(old.catches)
            new.finallyExpression =
                old.finallyExpression?.let {
                    copyElementPossiblyUnfinished(it)
                }
        }
        return new
    }

    open fun copyCatch(old: BirCatch): BirElement {
        val new = BirCatchImpl(
            sourceSpan = old.sourceSpan,
            catchParameter = copyElementPossiblyUnfinished(old.catchParameter),
            result = copyElementPossiblyUnfinished(old.result),
        )
        new.copyAuxData(old)
        return new
    }

    open fun copyTypeOperatorCall(old: BirTypeOperatorCall): BirElement {
        val new = BirTypeOperatorCallImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            operator = old.operator,
            argument = copyElementPossiblyUnfinished(old.argument),
            typeOperand = remapType(old.typeOperand),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyGetValue(old: BirGetValue): BirElement {
        val new = BirGetValueImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            origin = old.origin,
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copySetValue(old: BirSetValue): BirElement {
        val new = BirSetValueImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            symbol = remapSymbol(old.symbol),
            origin = old.origin,
            value = copyElementPossiblyUnfinished(old.value),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        return new
    }

    open fun copyVararg(old: BirVararg): BirElement {
        val new = BirVarargImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            varargElementType = remapType(old.varargElementType),
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.elements.copyElementsPossiblyUnfinished(old.elements)
        }
        return new
    }

    open fun copySpreadElement(old: BirSpreadElement): BirElement {
        val new = BirSpreadElementImpl(
            sourceSpan = old.sourceSpan,
            expression = copyElementPossiblyUnfinished(old.expression),
        )
        new.copyAuxData(old)
        return new
    }

    open fun copyWhen(old: BirWhen): BirElement {
        val new = BirWhenImpl(
            sourceSpan = old.sourceSpan,
            type = remapType(old.type),
            origin = old.origin,
        )
        new.copyAuxData(old)
        new.copyAttributes(old)
        deferInitialization(old, new) { old, new ->
            new.branches.copyElementsPossiblyUnfinished(old.branches)
        }
        return new
    }

    open fun copyBranch(old: BirBranch): BirElement {
        val new = BirBranchImpl(
            sourceSpan = old.sourceSpan,
            condition = copyElementPossiblyUnfinished(old.condition),
            result = copyElementPossiblyUnfinished(old.result),
        )
        new.copyAuxData(old)
        return new
    }

    open fun copyElseBranch(old: BirElseBranch): BirElement {
        val new = BirElseBranchImpl(
            sourceSpan = old.sourceSpan,
            condition = copyElementPossiblyUnfinished(old.condition),
            result = copyElementPossiblyUnfinished(old.result),
        )
        new.copyAuxData(old)
        return new
    }
}
