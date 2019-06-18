/*
 * Copyright 2018 The Android Open Source Project
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

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.ConstantValueGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import androidx.compose.plugins.kotlin.ComposableAnnotationChecker
import androidx.compose.plugins.kotlin.ComposeUtils.generateComposePackageName
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit

class ComposeObservePatcher(val context: JvmBackendContext) :
    IrElementTransformerVoid(),
    FileLoweringPass {

    private val typeTranslator =
        TypeTranslator(
            context.ir.symbols.externalSymbolTable,
            context.state.languageVersionSettings,
            context.builtIns
        ).apply {
            constantValueGenerator =
                ConstantValueGenerator(context.state.module, context.ir.symbols.externalSymbolTable)
            constantValueGenerator.typeTranslator = this
        }

    fun KotlinType.toIrType(): IrType = typeTranslator.translateType(this)

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {

        super.visitFunction(declaration)

        // Only insert observe scopes in non-empty composable function
        if (declaration.body == null) return declaration
        if (!isComposable(declaration)) return declaration

        // Do not insert an observe scope in an inline composable
        declaration.descriptor.findPsi()?.let { psi ->
            (psi as? KtFunctionLiteral)?.let {
                if (InlineUtil.isInlinedArgument(
                        it,
                        context.state.bindingContext,
                        true
                    )
                )
                    return declaration
            }
        }

        // Do not insert an observe scope if the funciton has a return result
        if (declaration.descriptor.returnType.let { it == null || !it.isUnit() }) return declaration

        val module = declaration.descriptor.module
        val observeFunctionDescriptor = module
            .getPackage(FqName(generateComposePackageName()))
            .memberScope
            .getContributedFunctions(
                Name.identifier("Observe"),
                NoLookupLocation.FROM_BACKEND
            ).single()

        val symbolTable = context.ir.symbols.externalSymbolTable

        val observeFunctionSymbol = symbolTable.referenceSimpleFunction(observeFunctionDescriptor)

        val type = observeFunctionDescriptor.valueParameters[0].type

        val lambdaDescriptor = AnonymousFunctionDescriptor(
            declaration.descriptor,
            Annotations.EMPTY,
            CallableMemberDescriptor.Kind.DECLARATION,
            SourceElement.NO_SOURCE,
            false
        ).apply {
            initialize(
                null,
                null,
                emptyList(),
                emptyList(),
                type,
                Modality.FINAL,
                Visibilities.LOCAL
            )
        }

        val irBuilder = context.createIrBuilder(declaration.symbol)

        return declaration.apply {
            body = irBuilder.irBlockBody {
                val fn = IrFunctionImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
                    IrSimpleFunctionSymbolImpl(lambdaDescriptor),
                    context.irBuiltIns.unitType
                ).also {
                    it.body = declaration.body
                }
                +fn
                +irCall(
                    observeFunctionSymbol,
                    observeFunctionDescriptor,
                    context.irBuiltIns.unitType
                ).also {
                    it.putValueArgument(
                        0, IrFunctionReferenceImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            type.toIrType(),
                            fn.symbol, fn.descriptor, 0, IrStatementOrigin.LAMBDA
                        )
                    )
                }
            }
        }
    }

    fun isComposable(declaration: IrFunction): Boolean {
        val tmpTrace =
            DelegatingBindingTrace(
                context.state.bindingContext, "tmp for composable analysis"
            )
        val composability =
            ComposableAnnotationChecker(ComposableAnnotationChecker.Mode.KTX_CHECKED)
                .analyze(
                tmpTrace,
                declaration.descriptor
            )
        return when (composability) {
            ComposableAnnotationChecker.Composability.NOT_COMPOSABLE -> false
            ComposableAnnotationChecker.Composability.MARKED -> true
            ComposableAnnotationChecker.Composability.INFERRED -> true
        }
    }
}
