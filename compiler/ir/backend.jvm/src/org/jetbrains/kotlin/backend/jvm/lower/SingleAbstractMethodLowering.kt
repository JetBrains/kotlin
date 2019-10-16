/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.localDeclarationsPhase
import org.jetbrains.kotlin.codegen.SamWrapperCodegen
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isNullConst
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

internal val singleAbstractMethodPhase = makeIrFilePhase(
    ::SingleAbstractMethodLowering,
    name = "SingleAbstractMethod",
    description = "Replace SAM conversions with instances of interface-implementing classes",
    prerequisite = setOf(localDeclarationsPhase)
)

class SingleAbstractMethodLowering(val context: CommonBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    private val cachedImplementations = mutableMapOf<IrType, IrClass>()
    private var enclosingClass: IrClass? = null

    // SAM wrappers are cached, either in the file class (if it exists), or in the top-level enclosing class.
    private inline fun <R> withCacheFor(irClass: IrClass?, block: () -> R): R {
        enclosingClass = enclosingClass ?: irClass
        val result = block()
        if (irClass != null && enclosingClass === irClass) {
            cachedImplementations.values.mapTo(irClass.declarations) {
                it.parent = irClass
                it
            }
            cachedImplementations.clear()
        }
        return result
    }

    override fun lower(irFile: IrFile) {
        val fileClass = irFile.declarations.filterIsInstance<IrClass>().find { it.origin == IrDeclarationOrigin.FILE_CLASS }
        withCacheFor(fileClass) { irFile.transformChildrenVoid() }
    }

    override fun visitClassNew(declaration: IrClass) =
        withCacheFor(declaration) { super.visitClassNew(declaration) }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        if (expression.operator != IrTypeOperator.SAM_CONVERSION)
            return super.visitTypeOperator(expression)
        val superType = expression.typeOperand
        val invokable = expression.argument.transform(this, null)
        context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).apply {
            // Do not generate a wrapper class for null, it has no invoke() anyway.
            if (invokable.isNullConst())
                return invokable

            // Coming from the frontend, every SAM interface is associated with exactly one function type
            // (see SamType.getKotlinFunctionType). That's why we can cache implementations just based on
            // the superType. Type parameters should have been erased before we get here.
            val implementation = cachedImplementations.getOrPut(superType) {
                createObjectProxy(superType, invokable.type)
            }

            return if (superType.isNullable() && invokable.type.isNullable()) {
                irBlock(invokable, null, superType) {
                    val invokableVariable = irTemporary(invokable)
                    val instance = irCall(implementation.constructors.single()).apply {
                        putValueArgument(0, irGet(invokableVariable))
                    }
                    irIfNull(superType, irGet(invokableVariable), irNull(), instance)
                }
            } else {
                irCall(implementation.constructors.single()).apply { putValueArgument(0, invokable) }
            }
        }
    }

    // Construct a class that wraps an invokable object into an implementation of an interface:
    //     class sam$n(private val invokable: F) : Interface { override fun method(...) = invokable(...) }
    private fun createObjectProxy(superType: IrType, invokableType: IrType): IrClass {
        val superClass = superType.classifierOrFail.owner as IrClass
        // The language documentation prohibits casting lambdas to classes, but if it was allowed,
        // the `irDelegatingConstructorCall` in the constructor below would need to be modified.
        assert(superClass.kind == ClassKind.INTERFACE) { "SAM conversion to an abstract class not allowed" }

        // TODO: In the scope of an inline function, we need to generate a *public* sam wrapper with a name of the form
        //       sam$i$superClassFqName$0 (note the additiona $i compared to the name below)
        val superFqName = superClass.fqNameWhenAvailable!!.asString().replace('.', '_')
        val wrapperName = Name.identifier("sam\$$superFqName${SamWrapperCodegen.SAM_WRAPPER_SUFFIX}")

        val subclass = buildClass {
            name = wrapperName
            origin = JvmLoweredDeclarationOrigin.GENERATED_SAM_IMPLEMENTATION
        }.apply {
            createImplicitParameterDeclarationWithWrappedDescriptor()
            // TODO convert all type parameters to upper bounds? See the kt11696 test.
            superTypes += superType
        }

        val field = subclass.addField {
            name = Name.identifier(SamWrapperCodegen.FUNCTION_FIELD_NAME)
            type = invokableType
            origin = subclass.origin
            visibility = Visibilities.PRIVATE
        }

        subclass.addConstructor {
            origin = subclass.origin
            isPrimary = true
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

        val superMethod = superClass.functions.single { it.modality == Modality.ABSTRACT }
        subclass.addFunction {
            name = superMethod.name
            returnType = superMethod.returnType
            visibility = superMethod.visibility
            origin = subclass.origin
        }.apply {
            overriddenSymbols += superMethod.symbol
            dispatchReceiverParameter = subclass.thisReceiver!!.copyTo(this)
            superMethod.valueParameters.mapTo(valueParameters) { it.copyTo(this) }
            val invokableClass = invokableType.classifierOrFail.owner as IrClass
            body = context.createIrBuilder(symbol).run {
                irExprBody(irCall(invokableClass.functions.single { it.name == OperatorNameConventions.INVOKE }).apply {
                    dispatchReceiver = irGetField(irGet(dispatchReceiverParameter!!), field)
                    valueParameters.forEachIndexed { i, parameter -> putValueArgument(i, irGet(parameter)) }
                })
            }
        }

        return subclass
    }
}
