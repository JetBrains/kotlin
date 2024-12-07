/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.ExpectSymbolTransformer
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.findCompatibleActualsForExpected

/**
 * Replaces `expect` symbols for which no `actual` counterpart exists with an `actual` stub in all files of the [IrModuleFragment]. The
 * implementation keeps track of generated stubs and only generates a single stub for each unique `expect` symbol.
 *
 * [stubOrphanedExpectSymbols] is used by the IDE bytecode tool window to allow compiling source files with `expect` declarations for which
 * the compiled module has no `actual` declaration. (The `actual` declaration would be defined in a module dependent on the compiled
 * module, but choosing this module is non-trivial due to possibly multiple implementations of the same `expect` symbol. In addition, when
 * generating bytecode for a single source file, the number of source files to compile should be kept low. Stubbing helps with that.)
 */
internal fun IrModuleFragment.stubOrphanedExpectSymbols(stubGenerator: DeclarationStubGenerator) {
    val transformer = StubOrphanedExpectSymbolTransformer(stubGenerator)
    files.forEach(transformer::visitFile)
}

private class StubOrphanedExpectSymbolTransformer(val stubGenerator: DeclarationStubGenerator) : ExpectSymbolTransformer() {

    private val stubbedClasses = mutableMapOf<ClassDescriptor, IrClassSymbol>()
    private val stubbedProperties = mutableMapOf<PropertyDescriptor, ActualPropertyResult>()
    private val stubbedConstructors = mutableMapOf<ClassConstructorDescriptor, IrConstructorSymbol>()
    private val stubbedFunctions = mutableMapOf<FunctionDescriptor, IrSimpleFunctionSymbol>()

    override fun getActualClass(descriptor: ClassDescriptor): IrClassSymbol? {
        if (!descriptor.isOrphanedExpect()) return null

        return stubbedClasses.getOrPut(descriptor) {
            stubGenerator.generateClassStub(FakeActualClassDescriptor(descriptor)).symbol
        }
    }

    override fun getActualProperty(descriptor: PropertyDescriptor): ActualPropertyResult? {
        if (!descriptor.isOrphanedExpect()) return null

        return stubbedProperties.getOrPut(descriptor) {
            val irProperty =
                stubGenerator.generatePropertyStub(FakeActualPropertyDescriptor(descriptor)).apply { ensureClassParent(descriptor) }
            val irGetter = descriptor.getter?.let(::getActualFunction)
            val irSetter = descriptor.setter?.let(::getActualFunction)
            ActualPropertyResult(irProperty.symbol, irGetter, irSetter)
        }
    }

    override fun getActualConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol? {
        if (!descriptor.isOrphanedExpect()) return null

        return stubbedConstructors.getOrPut(descriptor) {
            stubGenerator.generateConstructorStub(FakeActualClassConstructorDescriptor(descriptor)).symbol
        }
    }

    override fun getActualFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol? {
        if (!descriptor.isOrphanedExpect()) return null

        return stubbedFunctions.getOrPut(descriptor) {
            stubGenerator
                .generateFunctionStub(FakeActualFunctionDescriptor(descriptor), createPropertyIfNeeded = false)
                .apply { ensureClassParent(descriptor) }
                .symbol
        }
    }

    /**
     * Property getters and setters are not marked as `isExpect` even if the corresponding property is. However, we still need to stub such
     * getters and setters, so [isTargetDeclaration] allows it.
     */
    override fun isTargetDeclaration(declaration: IrDeclaration): Boolean =
        super.isTargetDeclaration(declaration) ||
                declaration is IrSimpleFunction && declaration.correspondingPropertySymbol?.owner?.isExpect == true

    /**
     * If an `actual` symbol exists, we shouldn't stub the `expect` symbol. This will be performed by
     * [org.jetbrains.kotlin.backend.common.lower.ExpectDeclarationsRemoveLowering] during lowering.
     */
    private fun MemberDescriptor.isOrphanedExpect(): Boolean = findCompatibleActualsForExpected(module).isEmpty()

    /**
     * [descriptor] should be the original descriptor, because the copied `actual` descriptor has no source.
     */
    private fun IrDeclaration.ensureClassParent(descriptor: MemberDescriptor) {
        if (parent !is IrClass) {
            parent = stubGenerator.generateOrGetFacadeClass(descriptor) ?: return
        }
    }

}

private class FakeActualClassDescriptor(original: ClassDescriptor) : ClassDescriptor by original {
    override fun isActual(): Boolean = true
    override fun isExpect(): Boolean = false

    override fun getSource(): SourceElement = SourceElement.NO_SOURCE
    override fun getOriginal(): ClassDescriptor = this
}

private class FakeActualPropertyDescriptor(original: PropertyDescriptor) : PropertyDescriptor by original {
    override fun isActual(): Boolean = true
    override fun isExpect(): Boolean = false

    override fun getSource(): SourceElement = SourceElement.NO_SOURCE
    override fun getOriginal(): PropertyDescriptor = this
}

private class FakeActualClassConstructorDescriptor(original: ClassConstructorDescriptor) : ClassConstructorDescriptor by original {
    override fun isActual(): Boolean = true
    override fun isExpect(): Boolean = false

    override fun getSource(): SourceElement = SourceElement.NO_SOURCE
    override fun getOriginal(): ClassConstructorDescriptor = this
}

private class FakeActualFunctionDescriptor(original: FunctionDescriptor) : FunctionDescriptor by original {
    override fun isActual(): Boolean = true
    override fun isExpect(): Boolean = false

    // `actual` functions are stubbed without providing a body. Hence, they may not be inlined, even if the `expect` function is marked as
    // `inline`. Given that inlining requires meaningful bodies (assuming the generated bytecode is of interest), it does not suffice to
    // just supply an empty body stub.
    override fun isInline(): Boolean = false

    override fun getSource(): SourceElement = SourceElement.NO_SOURCE
    override fun getOriginal(): FunctionDescriptor = this
}
