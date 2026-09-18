/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// These tests assert that the generated builders produce what `IrFactory.create*` produced, so they call the
// deprecated methods on purpose.
@file:OptIn(DeprecatedCompilerApi::class)

package org.jetbrains.kotlin.ir.declarations.builder

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.DeprecatedCompilerApi
import org.jetbrains.kotlin.ir.TestIrBuiltins
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Checks that the generated declaration builders produce the same declarations as the `IrFactory.create*` methods they mirror.
 */
class IrDeclarationBuildersTest {

    private val name = Name.identifier("Foo")

    /**
     * Counts how often [declarationCreated] runs. `IrFactoryImplForJsIC` and `IrFactoryImplForWasmIC` hook it to attach
     * `IdSignature`s, so a builder that constructed an `Ir*Impl` without going through the factory would silently break
     * incremental compilation.
     */
    private class CountingFactory : IrFactory(StageController()) {
        var declarationsCreated = 0

        override fun <T : IrDeclaration> T.declarationCreated(): T {
            declarationsCreated++
            return this
        }
    }

    @Test
    fun `buildClass defaults match createClass defaults`() {
        @OptIn(IrImplementationDetail::class)
        val expected = IrFactoryImpl.createClass(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            name = name,
            visibility = DescriptorVisibilities.PUBLIC,
            symbol = IrClassSymbolImpl(),
            kind = ClassKind.CLASS,
            modality = Modality.FINAL,
        )
        val actual = IrFactoryImpl.buildClass { name = this@IrDeclarationBuildersTest.name }

        assertEquals(expected.startOffset, actual.startOffset)
        assertEquals(expected.endOffset, actual.endOffset)
        assertEquals(expected.origin, actual.origin)
        assertEquals(expected.name, actual.name)
        assertEquals(expected.visibility, actual.visibility)
        assertEquals(expected.kind, actual.kind)
        assertEquals(expected.modality, actual.modality)
        assertEquals(expected.source, actual.source)
        assertEquals(expected.isExternal, actual.isExternal)
        assertEquals(expected.isCompanion, actual.isCompanion)
        assertEquals(expected.isInner, actual.isInner)
        assertEquals(expected.isData, actual.isData)
        assertEquals(expected.isValue, actual.isValue)
        assertEquals(expected.isExpect, actual.isExpect)
        assertEquals(expected.isFun, actual.isFun)
        assertEquals(expected.hasEnumEntries, actual.hasEnumEntries)
        assertSame(IrFactoryImpl, actual.factory)
    }

    @Test
    fun `buildClass mints a fresh symbol but lets the caller supply one`() {
        val first = IrFactoryImpl.buildClass { name = this@IrDeclarationBuildersTest.name }
        val second = IrFactoryImpl.buildClass { name = this@IrDeclarationBuildersTest.name }
        assertNotSame(first.symbol, second.symbol)
        assertSame(first, first.symbol.owner)

        // The gap that kept `SymbolTable`, the deserializer and fir2ir from using the hand-written builders at all.
        val ownSymbol = IrClassSymbolImpl()
        val withOwnSymbol = IrFactoryImpl.buildClass {
            name = this@IrDeclarationBuildersTest.name
            symbol = ownSymbol
        }
        assertSame(ownSymbol, withOwnSymbol.symbol)
    }

    @Test
    fun `buildClass assigns the properties the constructor does not take`() {
        val declaration = IrFactoryImpl.buildClass {
            name = this@IrDeclarationBuildersTest.name
            kind = ClassKind.INTERFACE
            modality = Modality.ABSTRACT
            source = SourceElement.NO_SOURCE
            isCompanion = true
            isInner = true
            isData = true
            isValue = true
            isExpect = true
            isFun = true
            isExternal = true
            hasEnumEntries = true
        }

        assertEquals(ClassKind.INTERFACE, declaration.kind)
        assertEquals(Modality.ABSTRACT, declaration.modality)
        assertEquals(
            listOf(true, true, true, true, true, true, true, true),
            listOf(
                declaration.isCompanion, declaration.isInner, declaration.isData, declaration.isValue,
                declaration.isExpect, declaration.isFun, declaration.isExternal, declaration.hasEnumEntries,
            ),
        )
    }

    @Test
    fun `a lateinit property is left uninitialized unless the builder was given a value`() {
        val withoutReturnType = IrFactoryImpl.buildSimpleFunction { name = this@IrDeclarationBuildersTest.name }
        assertThrows(UninitializedPropertyAccessException::class.java) { withoutReturnType.returnType }

        val withReturnType = IrFactoryImpl.buildSimpleFunction {
            name = this@IrDeclarationBuildersTest.name
            returnType = TestIrBuiltins.unitType
        }
        assertSame(TestIrBuiltins.unitType, withReturnType.returnType)
    }

    @Test
    fun `every declaration is created through the factory that built it`() {
        val factory = CountingFactory()

        val klass = factory.buildClass { name = this@IrDeclarationBuildersTest.name }
        val function = factory.buildSimpleFunction { name = this@IrDeclarationBuildersTest.name }
        val field = factory.buildField {
            name = this@IrDeclarationBuildersTest.name
            type = TestIrBuiltins.anyType
        }
        val property = factory.buildProperty { name = this@IrDeclarationBuildersTest.name }
        val valueParameter = factory.buildValueParameter {
            name = this@IrDeclarationBuildersTest.name
            type = TestIrBuiltins.anyType
        }

        assertEquals(5, factory.declarationsCreated)
        for (declaration in listOf(klass, function, field, property, valueParameter)) {
            assertSame(factory, declaration.factory)
        }
    }

    @Test
    fun `buildVariable needs no factory because IrVariableImpl stores none`() {
        val variable = buildVariable {
            name = this@IrDeclarationBuildersTest.name
            type = TestIrBuiltins.anyType
            isVar = true
        }

        assertEquals(name, variable.name)
        assertSame(TestIrBuiltins.anyType, variable.type)
        assertEquals(true, variable.isVar)
        assertFalse(variable.isConst)
        assertFalse(variable.isLateinit)
        assertSame(variable, variable.symbol.owner)
    }

    @Test
    fun `isFakeOverride falls back to the origin, as IrFactory does`() {
        // `createSimpleFunction` declares `isFakeOverride: Boolean = origin == FAKE_OVERRIDE`. A builder property's default
        // cannot see an `origin` the caller sets afterwards, so the builder resolves it in `build()` instead.
        @OptIn(IrImplementationDetail::class)
        val expected = IrFactoryImpl.createSimpleFunction(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.FAKE_OVERRIDE,
            name = name,
            visibility = DescriptorVisibilities.PUBLIC,
            isInline = false,
            isExpect = false,
            returnType = null,
            modality = Modality.FINAL,
            symbol = IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
        )
        assertTrue(expected.isFakeOverride)

        val derived = IrFactoryImpl.buildSimpleFunction {
            name = this@IrDeclarationBuildersTest.name
            origin = IrDeclarationOrigin.FAKE_OVERRIDE
        }
        assertEquals(expected.isFakeOverride, derived.isFakeOverride)

        // An explicit value still wins over the fallback, in both directions.
        val overridden = IrFactoryImpl.buildSimpleFunction {
            name = this@IrDeclarationBuildersTest.name
            origin = IrDeclarationOrigin.FAKE_OVERRIDE
            isFakeOverride = false
        }
        assertFalse(overridden.isFakeOverride)

        val plain = IrFactoryImpl.buildSimpleFunction { name = this@IrDeclarationBuildersTest.name }
        assertFalse(plain.isFakeOverride)
    }

    @Test
    fun `buildValueParameter defaults to a regular parameter`() {
        val parameter = IrFactoryImpl.buildValueParameter {
            name = this@IrDeclarationBuildersTest.name
            type = TestIrBuiltins.anyType
        }

        assertEquals(IrParameterKind.Regular, parameter.kind)
        assertFalse(parameter.isCrossinline)
        assertFalse(parameter.isNoinline)
        assertFalse(parameter.isHidden)
        assertFalse(parameter.isAssignable)
    }
}
