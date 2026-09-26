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
import org.jetbrains.kotlin.DeprecatedCompilerApi
import org.jetbrains.kotlin.ir.TestIrBuiltins
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
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
    fun `buildClass mints a fresh symbol but lets the caller supply one`() {
        val first = IrFactoryImpl.buildClass { name = this@IrDeclarationBuildersTest.name }
        val second = IrFactoryImpl.buildClass { name = this@IrDeclarationBuildersTest.name }
        assertNotSame(first.symbol, second.symbol)
        assertSame(first, first.symbol.owner)
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

    private val legacySimpleFunction: IrFunction = IrFactoryImpl.buildSimpleFunction {
        startOffset = 1
        endOffset = 2
        origin = IrDeclarationOrigin.FAKE_OVERRIDE
        name = Name.identifier("source")
        visibility = DescriptorVisibilities.INTERNAL
        isInline = true
        isExternal = true
        isExpect = true
        modality = Modality.ABSTRACT
        isFakeOverride = true
        isTailrec = true
        isSuspend = true
        isOperator = true
        isInfix = true
    }

    private val legacyConstructor: IrFunction = IrFactoryImpl.buildConstructor {
        startOffset = 3
        endOffset = 4
        origin = IrDeclarationOrigin.FAKE_OVERRIDE
        visibility = DescriptorVisibilities.PRIVATE
        isInline = true
        isExternal = true
        isExpect = true
        isPrimary = true
    }

    /** What the deprecated `IrFunctionBuilder.updateFrom` produces, starting from a builder with every flag set. */
    private fun legacyUpdateFrom(from: IrFunction) = IrFunctionBuilder().apply {
        name = this@IrDeclarationBuildersTest.name
        setAllFlags()
        updateFrom(from)
    }

    private fun IrFunctionBuilder.setAllFlags() {
        modality = Modality.OPEN
        isTailrec = true
        isSuspend = true
        isOperator = true
        isInfix = true
        isFakeOverride = true
        isPrimary = true
    }

    @Test
    fun `IrSimpleFunctionBuilder updateFrom an IrFunction copies what IrFunctionBuilder did`() {
        for (from in listOf(legacySimpleFunction, legacyConstructor)) {
            val expected = legacyUpdateFrom(from)
            val actual = IrSimpleFunctionBuilder().apply {
                name = this@IrDeclarationBuildersTest.name
                modality = Modality.OPEN
                isTailrec = true
                isSuspend = true
                isOperator = true
                isInfix = true
                isFakeOverride = true
                updateFrom(from)
            }

            assertEquals(
                listOf(
                    expected.startOffset, expected.endOffset, expected.origin, expected.name, expected.visibility,
                    expected.containerSource, expected.isInline, expected.isExternal, expected.isExpect, expected.modality,
                    expected.isTailrec, expected.isSuspend, expected.isOperator, expected.isInfix, expected.isFakeOverride,
                ),
                listOf(
                    actual.startOffset, actual.endOffset, actual.origin, actual.name, actual.visibility,
                    actual.containerSource, actual.isInline, actual.isExternal, actual.isExpect, actual.modality,
                    actual.isTailrec, actual.isSuspend, actual.isOperator, actual.isInfix, actual.isFakeOverride,
                ),
                "updateFrom(${from.name})",
            )
        }
    }

    @Test
    fun `IrConstructorBuilder updateFrom an IrFunction copies what IrFunctionBuilder did`() {
        for (from in listOf(legacySimpleFunction, legacyConstructor)) {
            val expected = legacyUpdateFrom(from)
            val actual = IrConstructorBuilder().apply {
                isPrimary = true
                updateFrom(from)
            }

            assertEquals(
                listOf(
                    expected.startOffset, expected.endOffset, expected.origin, expected.visibility, expected.containerSource,
                    expected.isInline, expected.isExternal, expected.isExpect, expected.isPrimary,
                ),
                listOf(
                    actual.startOffset, actual.endOffset, actual.origin, actual.visibility, actual.containerSource,
                    actual.isInline, actual.isExternal, actual.isExpect, actual.isPrimary,
                ),
                "updateFrom(${from.name})",
            )
        }
    }
}
