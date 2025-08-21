/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.JVM_NAME_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull

private var IrProperty.replacementForValueClasses: IrProperty? by irAttribute(copyByDefault = false)

abstract class MemoizedValueClassAbstractReplacements(
    protected val irFactory: IrFactory,
    protected val context: JvmBackendContext,
    protected val storageManager: LockBasedStorageManager
) {
    /**
     * Get a replacement for a function or a constructor.
     */
    fun getReplacementFunction(function: IrFunction) = getReplacementFunctionImpl(function)

    protected abstract val getReplacementFunctionImpl: (IrFunction) -> IrSimpleFunction?

    protected fun IrFunction.isRemoveAtSpecialBuiltinStub() =
        origin == IrDeclarationOrigin.IR_BUILTINS_STUB &&
                name.asString() == "remove" &&
                hasShape(dispatchReceiver = true, regularParameters = 1, parameterTypes = listOf(null, context.irBuiltIns.intType))

    protected fun IrFunction.isValueClassMemberFakeOverriddenFromJvmDefaultInterfaceMethod(): Boolean =
        this is IrSimpleFunction && isFakeOverride && modality != Modality.ABSTRACT &&
                context.cachedDeclarations.getClassFakeOverrideReplacement(this) == ClassFakeOverrideReplacement.None

    protected abstract fun createStaticReplacement(function: IrFunction): IrSimpleFunction
    protected abstract fun createMethodReplacement(function: IrFunction): IrSimpleFunction

    protected fun commonBuildReplacementInner(
        function: IrFunction,
        noFakeOverride: Boolean,
        body: IrFunction.() -> Unit,
        builderBody: IrFunctionBuilder.() -> Unit,
    ): IrSimpleFunction = irFactory.buildFun {
        updateFrom(function)
        builderBody()
        if (noFakeOverride) {
            isFakeOverride = false
        }
        returnType = function.returnType
    }.apply {
        parent = function.parent
        // Non-exposed methods and functions should not have @JvmExposeBoxed annotation, since we expect users to be able to
        // distinguish exposed functions via reflection.
        annotations = function.annotations.withoutJvmExposeBoxedAnnotation()
        copyTypeParameters(function.allTypeParameters)
        if (function.metadata != null) {
            metadata = function.metadata
            function.metadata = null
        }

        if (function is IrSimpleFunction) {
            copyAttributes(function)
            val propertySymbol = function.correspondingPropertySymbol
            if (propertySymbol != null) {
                val oldProperty = propertySymbol.owner
                val property = oldProperty::replacementForValueClasses.getOrSetIfNull {
                    irFactory.buildProperty {
                        name = oldProperty.name
                        updateFrom(oldProperty)
                    }.apply {
                        parent = oldProperty.parent
                        copyAttributes(oldProperty)
                        annotations = oldProperty.annotations
                        // In case this property is declared in an object in another file which is not yet lowered, its backing field will
                        // be made static later. We have to handle it here though, because this new property will be saved to the cache
                        // and reused when lowering the same call in all subsequent files, which would be incorrect if it was not lowered.
                        val newBackingField = context.cachedDeclarations.getStaticBackingField(oldProperty) ?: oldProperty.backingField
                        if (newBackingField != null) {
                            context.multiFieldValueClassReplacements.getMfvcFieldNode(newBackingField)
                            val fieldsToRemove = context.multiFieldValueClassReplacements.getFieldsToRemove(oldProperty.parentAsClass)
                            if (newBackingField !in fieldsToRemove) {
                                backingField = newBackingField
                            }
                        }
                    }
                }
                correspondingPropertySymbol = property.symbol
                when (function) {
                    oldProperty.getter -> property.getter = this
                    oldProperty.setter -> property.setter = this
                    else -> error("Orphaned property getter/setter: ${function.render()}")
                }
            }

            overriddenSymbols = replaceOverriddenSymbols(function)
        }

        body()
    }

    private val replaceOverriddenSymbolsImpl: (IrSimpleFunction) -> List<IrSimpleFunctionSymbol> =
        storageManager.createMemoizedFunction { irSimpleFunction ->
            irSimpleFunction.overriddenSymbols.map {
                computeOverrideReplacement(it.owner).symbol
            }
        }

    fun replaceOverriddenSymbols(function: IrSimpleFunction): List<IrSimpleFunctionSymbol> =
        if (function.overriddenSymbols.isEmpty()) listOf()
        else replaceOverriddenSymbolsImpl(function)

    abstract fun getReplacementForRegularClassConstructor(constructor: IrConstructor): IrConstructor?

    private fun computeOverrideReplacement(function: IrSimpleFunction): IrSimpleFunction =
        getReplacementFunction(function) ?: function.also {
            function.overriddenSymbols = replaceOverriddenSymbols(function)
        }

    protected fun IrSimpleFunction.overridesOnlyMethodsFromJava(): Boolean = allOverridden().all { it.isFromJava() }

    private fun String.escape() = asIterable().joinToString("") {
        when (it) {
            '-' -> "--"
            '$' -> "$$"
            '.' -> "-"
            else -> "$it"
        }
    }

    protected fun Name.withValueClassParameterNameIfNeeded(bound: IrClass, index: Int): Name =
        Name.identifier($$"$v$c$$${bound.fqNameWhenAvailable?.asString().orEmpty().escape()}$-$${asString()}$$$index")

    protected fun IrValueParameter.addOrInheritInlineClassPropertyNameParts(oldParameter: IrValueParameter) {
        when {
            hasFixedName -> return
            oldParameter.hasFixedName -> hasFixedName = true
            type.isNullable() -> return
            type.isInlineClassType() -> {
                name = name.withValueClassParameterNameIfNeeded(type.erasedUpperBound, index = 0)
                hasFixedName = true
            }
            else -> return
        }
    }
}

fun List<IrConstructorCall>.withoutJvmExposeBoxedAnnotation(): List<IrConstructorCall> =
    this.toMutableList().apply {
        removeAll {
            it.symbol.owner.returnType.classOrNull?.owner?.hasEqualFqName(JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME) == true
        }
    }

fun List<IrConstructorCall>.withJvmExposeBoxedAnnotation(declaration: IrDeclaration, context: JvmBackendContext): List<IrConstructorCall> {
    if (hasAnnotation(JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME)) {
        val jvmExposeBoxedAnnotation = findAnnotation(JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME)
        // If name is not provided, copy the name from @JvmName annotation, if the latter is present
        if (jvmExposeBoxedAnnotation?.arguments[0] == null) {
            val jvmName = declaration.getAnnotation(JVM_NAME_ANNOTATION_FQ_NAME)?.arguments[0]
            if (jvmName != null) {
                jvmExposeBoxedAnnotation?.arguments[0] = jvmName
            }
        }
        return this
    }
    // The declaration is not annotated with @JvmExposeBoxed - the annotation is on class
    // or -Xjvm-expose-boxed is specified. Add the annotation.
    val constructor = context.symbols.jvmExposeBoxedAnnotation.constructors.first()
    return this + IrConstructorCallImpl.fromSymbolOwner(
        constructor.owner.returnType,
        constructor
    ).apply {
        arguments.add(null)
    }
}

var IrValueParameter.hasFixedName: Boolean by irFlag(copyByDefault = true)
    internal set
