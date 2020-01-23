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

import androidx.compose.plugins.kotlin.ComposableAnnotationChecker
import androidx.compose.plugins.kotlin.ComposeFqNames
import androidx.compose.plugins.kotlin.KtxNameConventions
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.plugins.kotlin.irTrace
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.ConstantValueGenerator
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType

abstract class AbstractComposeLowering(
    val context: JvmBackendContext,
    val symbolRemapper: DeepCopySymbolRemapper
): IrElementTransformerVoid() {

    protected val typeTranslator =
        TypeTranslator(
            context.ir.symbols.externalSymbolTable,
            context.state.languageVersionSettings,
            context.builtIns
        ).apply {
            constantValueGenerator = ConstantValueGenerator(
                context.state.module,
                context.ir.symbols.externalSymbolTable
            )
            constantValueGenerator.typeTranslator = this
        }

    protected val builtIns = context.irBuiltIns

    protected val composerTypeDescriptor = context.state.module.findClassAcrossModuleDependencies(
        ClassId.topLevel(ComposeFqNames.Composer)
    ) ?: error("Cannot find the Composer class")

    private val symbolTable get() = context.ir.symbols.externalSymbolTable

    fun referenceFunction(descriptor: CallableDescriptor): IrFunctionSymbol {
        return symbolRemapper.getReferencedFunction(symbolTable.referenceFunction(descriptor))
    }

    fun referenceSimpleFunction(descriptor: SimpleFunctionDescriptor): IrSimpleFunctionSymbol {
        return symbolRemapper.getReferencedSimpleFunction(
            symbolTable.referenceSimpleFunction(descriptor)
        )
    }

    fun referenceConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol {
        return symbolRemapper.getReferencedConstructor(symbolTable.referenceConstructor(descriptor))
    }

    fun getTopLevelClass(fqName: FqName): IrClassSymbol {
        val descriptor = context.state.module.getPackage(fqName.parent()).memberScope
            .getContributedClassifier(
                fqName.shortName(), NoLookupLocation.FROM_BACKEND
            ) as ClassDescriptor? ?: error("Class is not found: $fqName")
        return symbolTable.referenceClass(descriptor)
    }

    fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)

    fun IrValueParameter.isComposerParam(): Boolean =
        (descriptor as? ValueParameterDescriptor)?.isComposerParam() ?: false

    fun ValueParameterDescriptor.isComposerParam(): Boolean =
        name == KtxNameConventions.COMPOSER_PARAMETER &&
                type.constructor.declarationDescriptor?.fqNameSafe == ComposeFqNames.Composer

    fun IrAnnotationContainer.hasComposableAnnotation(): Boolean {
        return annotations.hasAnnotation(ComposeFqNames.Composable)
    }

    fun IrCall.isTransformedComposableCall(): Boolean {
        return context.state.irTrace[ComposeWritableSlices.IS_COMPOSABLE_CALL, this] ?: false
    }

    private val composableChecker = ComposableAnnotationChecker()

    fun FunctionDescriptor.isComposable(): Boolean {
        val composability = composableChecker.analyze(context.state.bindingTrace, this)
        return when (composability) {
            ComposableAnnotationChecker.Composability.NOT_COMPOSABLE -> false
            ComposableAnnotationChecker.Composability.MARKED -> true
            ComposableAnnotationChecker.Composability.INFERRED -> true
        }
    }

    fun IrFunction.isComposable(): Boolean {
        return descriptor.isComposable()
    }

}