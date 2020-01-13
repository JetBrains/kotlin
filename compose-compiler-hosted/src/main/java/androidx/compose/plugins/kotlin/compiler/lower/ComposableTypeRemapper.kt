/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin.compiler.lower

import androidx.compose.plugins.kotlin.ComposeFqNames
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionBase
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolRenamer
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.replace


class DeepCopyIrTreeWithSymbolsPreservingMetadata(
    symbolRemapper: SymbolRemapper,
    typeRemapper: TypeRemapper,
    symbolRenamer: SymbolRenamer = SymbolRenamer.DEFAULT
) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper, symbolRenamer) {

    override fun visitClass(declaration: IrClass): IrClass {
        return super.visitClass(declaration).also { it.copyMetadataFrom(declaration) }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        return super.visitFunction(declaration).also { it.copyMetadataFrom(declaration) }
    }

    override fun visitProperty(declaration: IrProperty): IrProperty {
        return super.visitProperty(declaration).also { it.copyMetadataFrom(declaration) }
    }

    private fun IrElement.copyMetadataFrom(owner: IrMetadataSourceOwner) {
        when (this) {
            is IrPropertyImpl -> metadata = owner.metadata
            is IrFunctionBase -> metadata = owner.metadata
            is IrClassImpl -> metadata = owner.metadata
        }
    }
}

class ComposerTypeRemapper(
    private val context: JvmBackendContext,
    private val typeTranslator: TypeTranslator,
    private val composerTypeDescriptor: ClassDescriptor
) : TypeRemapper {

    lateinit var deepCopy: IrElementTransformerVoid

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {}

    override fun leaveScope() {}

    private fun IrType.isComposable(): Boolean {
        return annotations.hasAnnotation(ComposeFqNames.Composable)
    }

    private fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)

    override fun remapType(type: IrType): IrType {
        // TODO(lmr):
        // This is basically creating the KotlinType and then converting to an IrType. Consider
        // rewriting to just create the IrType directly, which would probably be more efficient.
        if (type !is IrSimpleType) return type
        if (!type.isFunction()) return type
        if (!type.isComposable()) return type
        val oldArguments = type.toKotlinType().arguments
        val newArguments =
            oldArguments.subList(0, oldArguments.size - 1) +
                    TypeProjectionImpl(composerTypeDescriptor.defaultType) +
                    oldArguments.last()

        return context
            .irBuiltIns
            .builtIns
            .getFunction(oldArguments.size) // return type is an argument, so this is n + 1
            .defaultType
            .replace(newArguments)
            .toIrType()
    }
}