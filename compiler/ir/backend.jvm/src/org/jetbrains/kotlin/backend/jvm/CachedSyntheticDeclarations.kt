/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.jvm.ir.isJvmInterface
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.org.objectweb.asm.Opcodes
import java.util.concurrent.ConcurrentHashMap

class CachedSyntheticDeclarations(private val context: JvmBackendContext) {
    private data class FieldKey(val fieldSymbol: IrFieldSymbol, val parent: IrDeclarationParent, val superQualifierSymbol: IrClassSymbol?)

    private data class FunctionKey(
        val functionSymbol: IrFunctionSymbol,
        val parent: IrDeclarationParent,
        val superQualifierSymbol: IrClassSymbol?
    )

    private val functionMap = ConcurrentHashMap<FunctionKey, IrFunctionSymbol>()
    private val getterMap = ConcurrentHashMap<FieldKey, IrSimpleFunctionSymbol>()
    private val setterMap = ConcurrentHashMap<FieldKey, IrSimpleFunctionSymbol>()

    fun getSyntheticFunctionAccessor(expression: IrFunctionAccessExpression, scopes: List<ScopeWithIr>): IrFunctionSymbol {
        return createAccessor(expression, scopes)
    }

    fun getSyntheticFunctionAccessor(reference: IrFunctionReference, scopes: List<ScopeWithIr>): IrFunctionSymbol {
        return createAccessor(reference.symbol, scopes, reference.dispatchReceiver?.type, null)
    }

    private fun createAccessor(expression: IrFunctionAccessExpression, scopes: List<ScopeWithIr>): IrFunctionSymbol =
        if (expression is IrCall)
            createAccessor(expression.symbol, scopes, expression.dispatchReceiver?.type, expression.superQualifierSymbol)
        else
            createAccessor(expression.symbol, scopes, null, null)

    private fun createAccessor(
        symbol: IrFunctionSymbol,
        scopes: List<ScopeWithIr>,
        dispatchReceiverType: IrType?,
        superQualifierSymbol: IrClassSymbol?
    ): IrFunctionSymbol {
        // Find the right container to insert the accessor. Simply put, when we call a function on a class A,
        // we also need to put its accessor into A. However, due to the way that calls are implemented in the
        // IR we generally need to look at the type of the dispatchReceiver *argument* in order to find the
        // correct class. Consider the following code:
        //
        //     fun run(f : () -> Int): Int = f()
        //
        //     open class A {
        //         private fun f() = 0
        //         fun g() = run { this.f() }
        //     }
        //
        //     class B : A {
        //         override fun g() = 1
        //         fun h() = run { super.g() }
        //     }
        //
        // We have calls to the private methods A.f from a generated Lambda subclass for the argument to `run`
        // in class A and a super call to A.g from a generated Lambda subclass in class B.
        //
        // In the first case, we need to produce an accessor in class A to access the private member of A.
        // Both the parent of the function f and the type of the dispatch receiver point to the correct class.
        // In the second case we need to call A.g from within class B, since this is the only way to invoke
        // a method of a superclass on the JVM. However, the IR for the call to super.g points directly to the
        // function g in class A. Confusingly, the `superQualifier` on this call also points to class A.
        // The only way to compute the actual enclosing class for the call is by looking at the type of the
        // dispatch receiver argument, which points to B.
        //
        // Beyond this, there can be accessors that are needed because other lowerings produce code calling
        // private methods (e.g., local functions for lambdas are private and called from generated
        // SAM wrapper classes). In this case we rely on the parent field of the called function.
        //
        // Finally, we need to produce accessors for calls to protected static methods coming from Java,
        // which we put in the closest enclosing class which has access to the method in question.

        val parent = symbol.owner.accessorParent(dispatchReceiverType?.classOrNull?.owner ?: symbol.owner.parent, scopes)

        // The key in the cache/map needs to be BOTH the symbol of the function being accessed AND the parent
        // of the accessor. Going from the above example, if we have another class C similar to B:
        //
        //     class C : A {
        //         override fun g() = 2
        //         fun i() = run { super.g() }
        //     }
        //
        // For the call to super.g in function i, the accessor to A.g must be produced in C. Therefore, we
        // cannot use the function symbol (A.g in the example) by itself as the key since there should be
        // one accessor per dispatch receiver (i.e., parent of the accessor).
        return functionMap.getOrPut(FunctionKey(symbol, parent, superQualifierSymbol)) {
            when (symbol) {
                is IrConstructorSymbol ->
                    symbol.owner.makeConstructorAccessor().symbol
                is IrSimpleFunctionSymbol ->
                    symbol.owner.makeSimpleFunctionAccessor(superQualifierSymbol, dispatchReceiverType, parent, scopes).symbol
                else -> error("Unknown subclass of IrFunctionSymbol")
            }
        }
    }

    private fun getSyntheticConstructorAccessor(
        declaration: IrConstructor,
        constructorToAccessorMap: ConcurrentHashMap<IrConstructor, IrConstructor>
    ): IrConstructor {
        return constructorToAccessorMap.getOrPut(declaration) {
            declaration.makeConstructorAccessor(JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR).also { accessor ->
                if (declaration.constructedClass.modality != Modality.SEALED) {
                    // There's a special case in the JVM backend for serializing the metadata of hidden
                    // constructors - we serialize the descriptor of the original constructor, but the
                    // signature of the accessor. We implement this special case in the JVM IR backend by
                    // attaching the metadata directly to the accessor. We also have to move all annotations
                    // to the accessor. Parameter annotations are already moved by the copyTo method.
                    if (declaration.metadata != null) {
                        accessor.metadata = declaration.metadata
                        declaration.metadata = null
                    }
                    accessor.annotations += declaration.annotations
                    declaration.annotations = emptyList()
                    declaration.valueParameters.forEach { it.annotations = emptyList() }
                }
            }
        }
    }

    private fun IrConstructor.makeConstructorAccessor(
        originForConstructorAccessor: IrDeclarationOrigin =
            JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
    ): IrConstructor {
        val source = this

        return factory.buildConstructor {
            origin = originForConstructorAccessor
            name = source.name
            visibility = DescriptorVisibilities.PUBLIC
        }.also { accessor ->
            accessor.parent = source.parent

            accessor.copyTypeParametersFrom(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.copyValueParametersToStatic(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            if (source.constructedClass.modality == Modality.SEALED) {
                for (accessorValueParameter in accessor.valueParameters) {
                    accessorValueParameter.annotations = emptyList()
                }
            }

            accessor.returnType = source.returnType.remapTypeParameters(source, accessor)

            accessor.addValueParameter(
                "constructor_marker".synthesizedString,
                context.ir.symbols.defaultConstructorMarker.defaultType.makeNullable(),
                JvmLoweredDeclarationOrigin.SYNTHETIC_MARKER_PARAMETER
            )

            accessor.body = IrExpressionBodyImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                createConstructorCall(accessor, source.symbol)
            )
        }
    }

    private fun createConstructorCall(accessor: IrConstructor, targetSymbol: IrConstructorSymbol) =
        IrDelegatingConstructorCallImpl.fromSymbolOwner(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            targetSymbol, targetSymbol.owner.parentAsClass.typeParameters.size + targetSymbol.owner.typeParameters.size
        ).also {
            copyAllParamsToArgs(it, accessor)
        }

    private fun IrSimpleFunction.makeSimpleFunctionAccessor(
        superQualifierSymbol: IrClassSymbol?, dispatchReceiverType: IrType?, parent: IrDeclarationParent, scopes: List<ScopeWithIr>
    ): IrSimpleFunction {
        val source = this

        return factory.buildFun {
            startOffset = parent.startOffset
            endOffset = parent.startOffset
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = source.accessorName(superQualifierSymbol, scopes)
            visibility = DescriptorVisibilities.PUBLIC
            modality = if (parent is IrClass && parent.isJvmInterface) Modality.OPEN else Modality.FINAL
            isSuspend = source.isSuspend // synthetic accessors of suspend functions are handled in codegen
        }.also { accessor ->
            accessor.parent = parent
            accessor.copyAttributes(source)
            accessor.copyTypeParametersFrom(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.copyValueParametersToStatic(source, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR, dispatchReceiverType)
            accessor.returnType = source.returnType.remapTypeParameters(source, accessor)

            accessor.body = IrExpressionBodyImpl(
                accessor.startOffset, accessor.startOffset,
                createSimpleFunctionCall(accessor, source.symbol, superQualifierSymbol)
            )
        }
    }

    private fun createSimpleFunctionCall(accessor: IrFunction, targetSymbol: IrSimpleFunctionSymbol, superQualifierSymbol: IrClassSymbol?) =
        IrCallImpl.fromSymbolOwner(
            accessor.startOffset,
            accessor.endOffset,
            accessor.returnType,
            targetSymbol, targetSymbol.owner.typeParameters.size,
            superQualifierSymbol = superQualifierSymbol
        ).also {
            copyAllParamsToArgs(it, accessor)
        }

    fun getSyntheticGetter(expression: IrGetField, scopes: List<ScopeWithIr>): IrSimpleFunctionSymbol {
        val dispatchReceiverType = expression.receiver?.type
        val dispatchReceiverClassSymbol = dispatchReceiverType?.classifierOrNull as? IrClassSymbol
        val symbol = expression.symbol
        val parent = symbol.owner.accessorParent(dispatchReceiverClassSymbol?.owner ?: symbol.owner.parent, scopes) as IrClass
        return getterMap.getOrPut(FieldKey(symbol, parent, expression.superQualifierSymbol)) {
            makeGetterAccessorSymbol(symbol, parent, expression.superQualifierSymbol)
        }
    }

    private fun makeGetterAccessorSymbol(
        fieldSymbol: IrFieldSymbol,
        parent: IrClass,
        superQualifierSymbol: IrClassSymbol?
    ): IrSimpleFunctionSymbol =
        context.irFactory.buildFun {
            startOffset = parent.startOffset
            endOffset = parent.startOffset
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = fieldSymbol.owner.accessorNameForGetter(superQualifierSymbol)
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            returnType = fieldSymbol.owner.type
        }.also { accessor ->
            accessor.parent = parent

            if (!fieldSymbol.owner.isStatic) {
                // Accessors are always to one's own fields.
                accessor.addValueParameter(
                    "\$this", parent.defaultType, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
                )
            }

            accessor.body = createAccessorBodyForGetter(fieldSymbol.owner, accessor, superQualifierSymbol)
        }.symbol

    private fun createAccessorBodyForGetter(
        targetField: IrField,
        accessor: IrSimpleFunction,
        superQualifierSymbol: IrClassSymbol?
    ): IrBody {
        val maybeDispatchReceiver =
            if (targetField.isStatic) null
            else IrGetValueImpl(accessor.startOffset, accessor.endOffset, accessor.valueParameters[0].symbol)
        return IrExpressionBodyImpl(
            accessor.startOffset, accessor.endOffset,
            IrGetFieldImpl(
                accessor.startOffset, accessor.endOffset,
                targetField.symbol,
                targetField.type,
                maybeDispatchReceiver,
                superQualifierSymbol = superQualifierSymbol
            )
        )
    }

    fun getSyntheticSetter(expression: IrSetField, scopes: List<ScopeWithIr>): IrSimpleFunctionSymbol {
        val dispatchReceiverType = expression.receiver?.type
        val dispatchReceiverClassSymbol = dispatchReceiverType?.classifierOrNull as? IrClassSymbol
        val symbol = expression.symbol
        val parent = symbol.owner.accessorParent(dispatchReceiverClassSymbol?.owner ?: symbol.owner.parent, scopes) as IrClass
        return setterMap.getOrPut(FieldKey(symbol, parent, expression.superQualifierSymbol)) {
            makeSetterAccessorSymbol(symbol, parent, expression.superQualifierSymbol)
        }
    }

    private fun makeSetterAccessorSymbol(
        fieldSymbol: IrFieldSymbol,
        parent: IrClass,
        superQualifierSymbol: IrClassSymbol?
    ): IrSimpleFunctionSymbol =
        context.irFactory.buildFun {
            startOffset = parent.startOffset
            endOffset = parent.startOffset
            origin = JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = fieldSymbol.owner.accessorNameForSetter(superQualifierSymbol)
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            returnType = context.irBuiltIns.unitType
        }.also { accessor ->
            accessor.parent = parent

            if (!fieldSymbol.owner.isStatic) {
                // Accessors are always to one's own fields.
                accessor.addValueParameter(
                    "\$this", parent.defaultType, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR
                )
            }

            accessor.addValueParameter("<set-?>", fieldSymbol.owner.type, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR)

            accessor.body = createAccessorBodyForSetter(fieldSymbol.owner, accessor, superQualifierSymbol)
        }.symbol

    private fun createAccessorBodyForSetter(
        targetField: IrField,
        accessor: IrSimpleFunction,
        superQualifierSymbol: IrClassSymbol?
    ): IrBody {
        val maybeDispatchReceiver =
            if (targetField.isStatic) null
            else IrGetValueImpl(accessor.startOffset, accessor.endOffset, accessor.valueParameters[0].symbol)
        val value = IrGetValueImpl(
            accessor.startOffset, accessor.endOffset,
            accessor.valueParameters[if (targetField.isStatic) 0 else 1].symbol
        )
        return IrExpressionBodyImpl(
            accessor.startOffset, accessor.endOffset,
            IrSetFieldImpl(
                accessor.startOffset, accessor.endOffset,
                targetField.symbol,
                maybeDispatchReceiver,
                value,
                context.irBuiltIns.unitType,
                superQualifierSymbol = superQualifierSymbol
            )
        )
    }

    private fun copyAllParamsToArgs(
        call: IrFunctionAccessExpression,
        syntheticFunction: IrFunction
    ) {
        var typeArgumentOffset = 0
        if (syntheticFunction is IrConstructor) {
            call.passTypeArgumentsFrom(syntheticFunction.parentAsClass)
            typeArgumentOffset = syntheticFunction.parentAsClass.typeParameters.size
        }
        call.passTypeArgumentsFrom(syntheticFunction, offset = typeArgumentOffset)

        var offset = 0
        val delegateTo = call.symbol.owner
        delegateTo.dispatchReceiverParameter?.let {
            call.dispatchReceiver =
                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, syntheticFunction.valueParameters[offset++].symbol)
        }

        delegateTo.extensionReceiverParameter?.let {
            call.extensionReceiver =
                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, syntheticFunction.valueParameters[offset++].symbol)
        }

        delegateTo.valueParameters.forEachIndexed { i, _ ->
            call.putValueArgument(
                i,
                IrGetValueImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    syntheticFunction.valueParameters[i + offset].symbol
                )
            )
        }
    }

    // In case of Java `protected static`, access could be done from a public inline function in the same package,
    // or a subclass of the Java class. Both cases require an accessor, which we cannot add to the Java class.
    private fun IrDeclarationWithVisibility.accessorParent(parent: IrDeclarationParent, scopes: List<ScopeWithIr>) =
        if (visibility == JavaDescriptorVisibilities.PROTECTED_STATIC_VISIBILITY) {
            val classes = scopes.map { it.irElement }.filterIsInstance<IrClass>()
            val companions = classes.mapNotNull(IrClass::companionObject)
            val objectsInScope =
                classes.flatMap { it.declarations.filter(IrDeclaration::isAnonymousObject).filterIsInstance<IrClass>() }
            val candidates = objectsInScope + companions + classes
            candidates.lastOrNull { parent is IrClass && it.isSubclassOf(parent) } ?: classes.last()
        } else {
            parent
        }

    private fun IrSimpleFunction.accessorName(superQualifier: IrClassSymbol?, scopes: List<ScopeWithIr>): Name {
        val jvmName = context.defaultMethodSignatureMapper.mapFunctionName(this)
        val currentClass = scopes.lastOrNull { it.scope.scopeOwnerSymbol is IrClassSymbol }
        val suffix = when {
            // Accessors for top level functions never need a suffix.
            isTopLevel -> ""

            // The only function accessors placed on interfaces are for private functions and JvmDefault implementations.
            // The two cannot clash.
            currentClass?.irElement?.let { element ->
                element is IrClass && element.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS && element.parentAsClass == parentAsClass
            } ?: false -> if (!DescriptorVisibilities.isPrivate(visibility)) "\$jd" else ""

            // Accessor for _s_uper-qualified call
            superQualifier != null -> "\$s" + superQualifier.owner.syntheticAccessorToSuperSuffix()

            // Access to protected members that need an accessor must be because they are inherited,
            // hence accessed on a _s_upertype. If what is accessed is static, we can point to different
            // parts of the inheritance hierarchy and need to distinguish with a suffix.
            isStatic && visibility.isProtected -> "\$s" + parentAsClass.syntheticAccessorToSuperSuffix()

            else -> ""
        }
        return Name.identifier("access\$$jvmName$suffix")
    }

    private fun IrField.accessorNameForGetter(superQualifierSymbol: IrClassSymbol?): Name {
        val getterName = JvmAbi.getterName(name.asString())
        return Name.identifier("access\$$getterName\$${fieldAccessorSuffix(superQualifierSymbol)}")
    }

    private fun IrField.accessorNameForSetter(superQualifierSymbol: IrClassSymbol?): Name {
        val setterName = JvmAbi.setterName(name.asString())
        return Name.identifier("access\$$setterName\$${fieldAccessorSuffix(superQualifierSymbol)}")
    }

    private fun IrField.fieldAccessorSuffix(superQualifierSymbol: IrClassSymbol?): String {
        // Special _c_ompanion _p_roperty suffix for accessing companion backing field moved to outer
        if (origin == JvmLoweredDeclarationOrigin.COMPANION_PROPERTY_BACKING_FIELD && !parentAsClass.isCompanion) {
            return "cp"
        }

        if (superQualifierSymbol != null) {
            return "p\$s${superQualifierSymbol.owner.syntheticAccessorToSuperSuffix()}"
        }

        // Accesses to static protected fields that need an accessor must be due to being inherited, hence accessed on a
        // _s_upertype. If the field is static, the super class the access is on can be different and therefore
        // we generate a suffix to distinguish access to field with different receiver types in the super hierarchy.
        return "p" + if (isStatic && visibility.isProtected) "\$s" + parentAsClass.syntheticAccessorToSuperSuffix() else ""
    }

    private fun IrClass.syntheticAccessorToSuperSuffix(): String =
        // TODO: change this to `fqNameUnsafe.asString().replace(".", "_")` as soon as we're ready to break compatibility with pre-KT-21178 code
        name.asString().hashCode().toString()

    private val DescriptorVisibility.isProtected
        get() = AsmUtil.getVisibilityAccessFlag(delegate) == Opcodes.ACC_PROTECTED

    fun isOrShouldBeHiddenSinceHasMangledParams(constructor: IrConstructor): Boolean {
        if (constructor in context.hiddenConstructorsWithMangledParams.keys) return true
        return constructor.isOrShouldBeHiddenDueToOrigin &&
                !DescriptorVisibilities.isPrivate(constructor.visibility) &&
                !constructor.constructedClass.isValue &&
                (context.multiFieldValueClassReplacements.originalConstructorForConstructorReplacement[constructor] ?: constructor).hasMangledParameters() &&
                !constructor.constructedClass.isAnonymousObject
    }

    fun isOrShouldBeHiddenAsSealedClassConstructor(constructor: IrConstructor): Boolean {
        if (constructor in context.hiddenConstructorsOfSealedClasses.keys) return true
        return constructor.isOrShouldBeHiddenDueToOrigin && constructor.visibility != DescriptorVisibilities.PUBLIC && constructor.constructedClass.modality == Modality.SEALED
    }

    private val IrConstructor.isOrShouldBeHiddenDueToOrigin: Boolean
        get() = !(origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
                origin == JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR ||
                origin == JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR_FOR_HIDDEN_CONSTRUCTOR ||
                origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB)

    fun getSyntheticConstructorWithMangledParams(declaration: IrConstructor) =
        getSyntheticConstructorAccessor(declaration, context.hiddenConstructorsWithMangledParams)

    fun getSyntheticConstructorOfSealedClass(declaration: IrConstructor) =
        getSyntheticConstructorAccessor(declaration, context.hiddenConstructorsOfSealedClasses)
}