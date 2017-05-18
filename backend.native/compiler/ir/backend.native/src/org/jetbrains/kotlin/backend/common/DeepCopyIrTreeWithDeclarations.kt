/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.konan.ir.IrReturnableBlock
import org.jetbrains.kotlin.backend.konan.ir.IrReturnableBlockImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetVariableImpl
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolsRemapper
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Copies IR tree with descriptors of all declarations inside;
 * updates the references to these declarations.
 */
@Deprecated("Creates unbound symbols")
open class DeepCopyIrTreeWithDeclarations : DeepCopyIrTree() {

    private fun DeclarationDescriptor.notSupported(): Nothing = TODO("${this}")

    override fun mapModuleDescriptor(descriptor: ModuleDescriptor) = descriptor.notSupported()
    override fun mapPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor) = descriptor.notSupported()
    override fun mapClassDeclaration(descriptor: ClassDescriptor) = descriptor.notSupported()
    override fun mapTypeAliasDeclaration(descriptor: TypeAliasDescriptor) = descriptor.notSupported()
    override fun mapFunctionDeclaration(descriptor: FunctionDescriptor) = descriptor.notSupported()
    override fun mapConstructorDeclaration(descriptor: ClassConstructorDescriptor) = descriptor.notSupported()
    override fun mapPropertyDeclaration(descriptor: PropertyDescriptor) = descriptor.notSupported()
    override fun mapLocalPropertyDeclaration(descriptor: VariableDescriptorWithAccessors) = descriptor.notSupported()
    override fun mapEnumEntryDeclaration(descriptor: ClassDescriptor) = descriptor.notSupported()

    val copiedVariables = mutableMapOf<VariableDescriptor, VariableDescriptor>()

    override fun mapVariableDeclaration(descriptor: VariableDescriptor): VariableDescriptor {
        // TODO: how to ensure that the variable is not visible from outside of the transformed IR?

        if (descriptor is VariableDescriptorWithAccessors && (descriptor.getter != null || descriptor.setter != null)) {
            TODO("$descriptor with accessors")
        }

        val newDescriptor = LocalVariableDescriptor(
                /* containingDeclaration = */ descriptor.containingDeclaration,
                /* annotations = */ descriptor.annotations,
                /* name = */ descriptor.name,
                /* type = */ descriptor.type,
                /* mutable = */ descriptor.isVar,
                /* isDelegated = */ false,
                /* source = */ descriptor.source
        )

        assert (descriptor !in copiedVariables)
        copiedVariables[descriptor] = newDescriptor

        return newDescriptor
    }

    // Note: the reference to a variable can be traversed only after the declaration of that variable,
    // so it is correct to map only references whose descriptors have copies.
    // However such approach can be incorrect when copying functions, classes etc.

    override fun mapValueReference(descriptor: ValueDescriptor) =
            copiedVariables[descriptor] ?: descriptor

    override fun mapVariableReference(descriptor: VariableDescriptor) =
            copiedVariables[descriptor] ?: descriptor

    override fun visitBlock(expression: IrBlock): IrBlock {
        return if (expression is IrReturnableBlock) {
            IrReturnableBlockImpl(
                    startOffset = expression.startOffset,
                    endOffset   = expression.endOffset,
                    type        = expression.type,
                    descriptor  = expression.descriptor,
                    origin      = mapStatementOrigin(expression.origin),
                    statements  = expression.statements.map { it.transform(this, null) }
            )
        } else {
            super.visitBlock(expression)
        }
    }

    override fun getNonTransformedLoop(irLoop: IrLoop): IrLoop {
        return irLoop
    }
}

fun IrElement.deepCopyWithVariablesImpl(): IrElement {
    // FIXME: support non-transformed loops

    val remapper = DeepCopySymbolsRemapper()
    acceptVoid(remapper)

    val variableDescriptorReplacer = object : IrElementTransformerVoid() {

        val copiedVariables = mutableMapOf<IrVariableSymbol, IrVariableSymbol>()

        override fun visitVariable(declaration: IrVariable): IrStatement {
            declaration.transformChildrenVoid()

            val descriptor = declaration.descriptor
            val newDescriptor = LocalVariableDescriptor(
                    /* containingDeclaration = */ descriptor.containingDeclaration,
                    /* annotations = */ descriptor.annotations,
                    /* name = */ descriptor.name,
                    /* type = */ descriptor.type,
                    /* mutable = */ descriptor.isVar,
                    /* isDelegated = */ false,
                    /* source = */ descriptor.source
            )
            val newSymbol = IrVariableSymbolImpl(newDescriptor)
            copiedVariables[declaration.symbol] = newSymbol

            return with(declaration) {
                IrVariableImpl(startOffset, endOffset, origin, newSymbol).also {
                    it.initializer = initializer
                }
            }

        }

        override fun visitValueAccess(expression: IrValueAccessExpression): IrExpression {
            assert(expression.symbol !in copiedVariables)
            return super.visitValueAccess(expression)
        }

        override fun visitGetValue(expression: IrGetValue): IrExpression {
            val newSymbol = copiedVariables[expression.symbol] ?: return super.visitGetValue(expression)

            expression.transformChildrenVoid()
            return with(expression) {
                IrGetValueImpl(startOffset, endOffset, newSymbol, origin)
            }
        }

        override fun visitSetVariable(expression: IrSetVariable): IrExpression {
            val newSymbol = copiedVariables[expression.symbol] ?: return super.visitSetVariable(expression)

            expression.transformChildrenVoid()
            return with(expression) {
                IrSetVariableImpl(startOffset, endOffset, newSymbol, value, origin)
            }
        }

    }

    return this.transform(DeepCopyIrTreeWithSymbols(remapper), null).transform(variableDescriptorReplacer, null)
}

inline fun <reified T : IrElement> T.deepCopyWithVariables(): T =
        this.deepCopyWithVariablesImpl() as T