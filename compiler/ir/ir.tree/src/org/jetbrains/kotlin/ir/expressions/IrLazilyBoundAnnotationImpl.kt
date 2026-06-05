/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.DeprecatedCompilerApi
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrProvider
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * A special "bridge" implementation of IrAnnotation. It can be initialized with the "new API" (annotationClassSymbol and argumentMapping,
 * see KT-74200), then transforms it to the "old API" (symbol, arguments) upon first access.
 * It may be used when the information required for the old API is not available when creating an instance of IrAnnotation, but it still
 * may be used later by a code not yet supporting the new API. In particular, it is purposed for use during the linkage process,
 * when KotlinIrLinker.getDeclaration (required to get the annotation's constructor from class symbol) is not yet safe to call.
 */
class IrLazilyBoundAnnotationImpl(
    override var startOffset: Int,
    override var endOffset: Int,
    override var type: IrType,
    override var origin: IrStatementOrigin?,
    override var source: SourceElement,
    override var constructorTypeArgumentsCount: Int,
    override val classSymbol: IrClassSymbol,
    argumentMapping: Map<Name, IrExpression>,
    private val linker: IrProvider,
) : IrAnnotation() {
    override var argumentMapping: Map<Name, IrExpression>? = argumentMapping

    override var attributeOwnerId: IrElement = this
    override val typeArguments: MutableList<IrType?> = ArrayList(0)

    private fun transitionToCtorSymbolBasedApi() = synchronized(this) {
        if (_symbol == null) {
            val annotationClass = linker.getDeclaration(classSymbol) as IrClass
            val constructor = annotationClass.primaryConstructor!!
            _symbol = constructor.symbol

            val argumentList = List(constructor.parameters.size) { index ->
                val parameter = constructor.parameters[index]
                argumentMapping!![parameter.name]
            }
            super.arguments.assignFrom(argumentList)

            // The old and new APIs are not kept in sync, so remove the data from the new API so that there is only one source of truth
            // to avoid potential inconsistencies.
            // It's fine because, at this point, all the code that can handle the new API should also be able to handle the old one.
            argumentMapping = null
        }
    }

    private var _symbol: IrConstructorSymbol? = null

    @property:DeprecatedCompilerApi(deprecatedSince = org.jetbrains.kotlin.CompilerVersionOfApiDeprecation._2_4_20)
    override var symbol: IrConstructorSymbol
        get() {
            transitionToCtorSymbolBasedApi()
            return _symbol!!
        }
        set(value) {
            transitionToCtorSymbolBasedApi()
            _symbol = value
        }

    override val arguments: IrMemberAccessExpression<IrFunctionSymbol>.ValueArgumentsList
        get() {
            transitionToCtorSymbolBasedApi()
            return super.arguments
        }

    companion object
}
