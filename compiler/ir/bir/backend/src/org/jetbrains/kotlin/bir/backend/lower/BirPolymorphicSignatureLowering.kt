/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.builders.build
import org.jetbrains.kotlin.bir.backend.builders.copyFlagsFrom
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.declarations.BirValueParameter
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.expressions.impl.BirCallImpl
import org.jetbrains.kotlin.bir.getBackReferences

import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.types.isNullableAny
import org.jetbrains.kotlin.bir.util.ancestors
import org.jetbrains.kotlin.bir.util.copyTypeParametersFrom
import org.jetbrains.kotlin.bir.util.dump
import org.jetbrains.kotlin.bir.util.hasAnnotation
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.checkers.PolymorphicSignatureCallChecker

context(JvmBirBackendContext)
class BirPolymorphicSignatureLowering : BirLoweringPhase() {
    private val PolymorphicSignatureAnnotation by lz { birBuiltIns.findClass(PolymorphicSignatureCallChecker.polymorphicSignatureFqName) }

    override fun lower(module: BirModuleFragment) {
        getAllElementsOfClass(BirSimpleFunction, true).forEach { function ->
            if (PolymorphicSignatureAnnotation?.let { function.hasAnnotation(it) } == true) {
                function.getBackReferences(BirCall.symbol).forEach { call ->
                    val castReturnType = call.findCoercionType()
                    val newCall = transformPolymorphicCall(call, castReturnType)

                    val parent = call.parent
                    if (parent is BirTypeOperatorCall && parent.argument == newCall && parent.isCast()) {
                        parent.replaceWith(newCall)
                    } else {
                        call.replaceWith(newCall)
                    }
                }
            }
        }
    }

    private fun BirCall.findCoercionType(): BirType? {
        return ancestors()
            .takeWhile { element ->
                when (val parent = element.parent) {
                    null -> false
                    is BirWhen, is BirTypeOperatorCall -> true
                    is BirTry -> element == parent.tryResult || element in parent.catches
                    is BirContainerExpression -> element == parent.statements.lastOrNull()
                    else -> false
                }
            }
            .filterIsInstance<BirTypeOperatorCall>()
            .firstOrNull { it.isCast() }
            ?.typeOperand
    }

    private fun BirTypeOperatorCall.isCast(): Boolean =
        operator != IrTypeOperator.INSTANCEOF && operator != IrTypeOperator.NOT_INSTANCEOF

    private fun transformPolymorphicCall(oldCall: BirCall, castReturnType: BirType?): BirCallImpl {
        val function = oldCall.symbol.owner
        assert(function.valueParameters.singleOrNull()?.varargElementType != null) {
            "@PolymorphicSignature methods should only have a single vararg argument: ${oldCall.dump()}"
        }

        val values = (oldCall.valueArguments[0] as BirVararg?)?.elements?.map {
            when (it) {
                is BirExpression -> it
                is BirSpreadElement -> it.expression // `*xs` acts as `xs` (for compatibility?)
                else -> throw AssertionError("unknown BirVarargElement: $it")
            }
        } ?: listOf()
        oldCall.valueArguments[0] = null

        val fakeFunction = BirSimpleFunction.build {
            copyFlagsFrom(function)
            name = function.name
            origin = JvmLoweredDeclarationOrigin.POLYMORPHIC_SIGNATURE_INSTANTIATION
            returnType = if (function.returnType.isNullableAny()) castReturnType ?: function.returnType else function.returnType
            copyTypeParametersFrom(function)
            dispatchReceiverParameter = function.dispatchReceiverParameter
            extensionReceiverParameter = function.extensionReceiverParameter
            values.forEachIndexed { i, value ->
                valueParameters += BirValueParameter.build {
                    name = Name.identifier("\$$i")
                    type = value!!.type
                    origin = JvmLoweredDeclarationOrigin.POLYMORPHIC_SIGNATURE_INSTANTIATION
                    index = i
                }
            }
        }
        return BirCall.build {
            sourceSpan = oldCall.sourceSpan
            type = fakeFunction.returnType
            symbol = fakeFunction
            origin = oldCall.origin
            superQualifierSymbol = oldCall.superQualifierSymbol
            typeArguments = oldCall.typeArguments
            dispatchReceiver = oldCall.dispatchReceiver
            extensionReceiver = oldCall.extensionReceiver
            valueArguments += values
        }
    }
}