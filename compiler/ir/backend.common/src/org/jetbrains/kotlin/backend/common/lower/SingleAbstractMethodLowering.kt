/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

abstract class SingleAbstractMethodLowering(val context: CommonBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    // SAM wrappers are cached, either in the file class (if it exists), or in a top-level enclosing class.
    // In the latter case, the names of SAM wrappers depend on the order of classes in the file. For example:
    //
    //    class A {
    //      fun f(run: () -> Unit) = Runnable(run)
    //    }
    //
    //    class B {
    //      fun g(run: () -> Unit) = Runnable(run)
    //      fun h(p: (String) -> Boolean) = Predicate(p)
    //    }
    //
    // This code creates two SAM wrappers, `A$sam$java_lang_Runnable$0`, which is used in both
    // `A.f` and `B.g`, as well as `B$sam$java_util_function_Predicate$0`, which is used in `B.h`.
    //
    // Additionally, we need to cache SAM wrappers inside inline functions separately from those
    // outside of inline functions. Outside of inline functions we generate package private wrappers
    // with name prefix "sam$". In the scope of an inline function we generate public wrappers with
    // name prefix "sam$i$".
    //
    // Coming from the frontend, every SAM interface is associated with exactly one function type
    // (see SamType.getKotlinFunctionType). This is why we can cache implementations just based on
    // the superType.
    private val cachedImplementations = mutableMapOf<IrType, IrClass>()
    private val inlineCachedImplementations = mutableMapOf<IrType, IrClass>()
    private var enclosingContainer: IrDeclarationContainer? = null

    open val privateGeneratedWrapperVisibility: Visibility
        get() = Visibilities.PRIVATE

    abstract fun getSuperTypeForWrapper(typeOperand: IrType): IrType

    override fun lower(irFile: IrFile) {
        cachedImplementations.clear()
        inlineCachedImplementations.clear()
        enclosingContainer = irFile.declarations.filterIsInstance<IrClass>().find { it.isFileClass }
            ?: irFile
        irFile.transformChildrenVoid()

        for (wrapper in cachedImplementations.values + inlineCachedImplementations.values) {
            val parentClass = wrapper.parent as IrDeclarationContainer
            parentClass.declarations += wrapper
        }
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        val prevContainer = enclosingContainer
        if (prevContainer == null || prevContainer is IrFile)
            enclosingContainer = declaration
        super.visitClassNew(declaration)
        enclosingContainer = prevContainer
        return declaration
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        if (expression.operator != IrTypeOperator.SAM_CONVERSION)
            return super.visitTypeOperator(expression)
        // TODO: there must be exactly one wrapper per Java interface; ideally, if the interface has generic
        //       parameters, so should the wrapper. Currently, we just erase them and generate something that
        //       erases to the same result at codegen time.
        val erasedSuperType = getSuperTypeForWrapper(expression.typeOperand)
        val superType = if (expression.typeOperand.isNullable()) erasedSuperType.makeNullable() else erasedSuperType
        val invokable = expression.argument.transform(this, null)
        context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).apply {
            // Do not generate a wrapper class for null, it has no invoke() anyway.
            if (invokable.isNullConst())
                return invokable

            val inInlineFunctionScope = allScopes.any { scope -> (scope.irElement as? IrFunction)?.isInline ?: false }
            val cache = if (inInlineFunctionScope) inlineCachedImplementations else cachedImplementations
            val implementation = cache.getOrPut(superType) {
                createObjectProxy(superType, inInlineFunctionScope)
            }

            return if (superType.isNullable() && invokable.type.isNullable()) {
                irBlock(invokable, null, superType) {
                    val invokableVariable = irTemporary(invokable)
                    val instance = irCall(implementation.constructors.single()).apply {
                        putValueArgument(0, irGet(invokableVariable))
                    }
                    +irIfNull(superType, irGet(invokableVariable), irNull(), instance)
                }
            } else if (invokable !is IrGetValue) {
                // Hack for the JVM inliner: since the SAM wrappers might be regenerated, avoid putting complex logic
                // between the creation of the wrapper and the call of its `<init>`. `MethodInliner` tends to break
                // otherwise, e.g. if the argument constructs an anonymous object, resulting in new-new-<init>-<init>.
                // (See KT-21781 for a similar problem with anonymous object constructor arguments.)
                irBlock(invokable, null, superType) {
                    val invokableVariable = irTemporary(invokable)
                    +irCall(implementation.constructors.single()).apply { putValueArgument(0, irGet(invokableVariable)) }
                }
            } else {
                irCall(implementation.constructors.single()).apply { putValueArgument(0, invokable) }
            }
        }
    }

    private val SAM_WRAPPER_SUFFIX = "$0"
    private val FUNCTION_FIELD_NAME = "function"

    // Construct a class that wraps an invokable object into an implementation of an interface:
    //     class sam$n(private val invokable: F) : Interface { override fun method(...) = invokable(...) }
    private fun createObjectProxy(superType: IrType, generatePublicWrapper: Boolean): IrClass {
        val superClass = superType.classifierOrFail.owner as IrClass
        // The language documentation prohibits casting lambdas to classes, but if it was allowed,
        // the `irDelegatingConstructorCall` in the constructor below would need to be modified.
        assert(superClass.kind == ClassKind.INTERFACE) { "SAM conversion to an abstract class not allowed" }

        val superFqName = superClass.fqNameWhenAvailable!!.asString().replace('.', '_')
        val inlinePrefix = if (generatePublicWrapper) "\$i" else ""
        val wrapperName = Name.identifier("sam$inlinePrefix\$$superFqName$SAM_WRAPPER_SUFFIX")
        val superMethod = superClass.functions.single { it.modality == Modality.ABSTRACT }
        // TODO: have psi2ir cast the argument to the correct function type. Also see the TODO
        //       about type parameters in `visitTypeOperator`.
        val wrappedFunctionClass =
            if (superMethod.isSuspend)
                context.ir.symbols.suspendFunctionN(superMethod.valueParameters.size).owner
            else
                context.ir.symbols.functionN(superMethod.valueParameters.size).owner
        val wrappedFunctionType = wrappedFunctionClass.defaultType

        val wrapperVisibility = if (generatePublicWrapper) Visibilities.PUBLIC else privateGeneratedWrapperVisibility
        val subclass = buildClass {
            name = wrapperName
            origin = IrDeclarationOrigin.GENERATED_SAM_IMPLEMENTATION
            visibility = wrapperVisibility
        }.apply {
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes += superType
            parent = enclosingContainer!!
        }

        val field = subclass.addField {
            name = Name.identifier(FUNCTION_FIELD_NAME)
            type = wrappedFunctionType
            origin = subclass.origin
            visibility = Visibilities.PRIVATE
        }

        subclass.addConstructor {
            origin = subclass.origin
            isPrimary = true
            visibility = wrapperVisibility
        }.apply {
            val parameter = addValueParameter {
                name = field.name
                type = field.type
                origin = subclass.origin
            }

            body = context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                +irSetField(irGet(subclass.thisReceiver!!), field, irGet(parameter))
                +IrInstanceInitializerCallImpl(startOffset, endOffset, subclass.symbol, context.irBuiltIns.unitType)
            }
        }

        subclass.addFunction {
            name = superMethod.name
            returnType = superMethod.returnType
            visibility = superMethod.visibility
            origin = subclass.origin
            isSuspend = superMethod.isSuspend
        }.apply {
            overriddenSymbols += superMethod.symbol
            dispatchReceiverParameter = subclass.thisReceiver!!.copyTo(this)
            valueParameters = superMethod.valueParameters.map { it.copyTo(this) }
            body = context.createIrBuilder(symbol).irBlockBody {
                +irReturn(irCall(wrappedFunctionClass.functions.single { it.name == OperatorNameConventions.INVOKE }).apply {
                    dispatchReceiver = irGetField(irGet(dispatchReceiverParameter!!), field)
                    valueParameters.forEachIndexed { i, parameter -> putValueArgument(i, irGet(parameter)) }
                })
            }
        }

        return subclass
    }
}
