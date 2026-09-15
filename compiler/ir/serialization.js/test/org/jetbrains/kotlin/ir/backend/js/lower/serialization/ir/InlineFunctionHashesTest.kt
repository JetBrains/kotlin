/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.js.InlineFunctionSymbol
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class InlineFunctionHashesTest {
    private val module = IrModuleFragmentImpl(
        ModuleDescriptorImpl(Name.special("<test>"), LockBasedStorageManager("InlineFunctionHashesTest"), DefaultBuiltIns.Instance)
    )
    private val file = IrFileImpl(
        NaiveSourceBasedFileEntryImpl("test.kt", intArrayOf(0), maxOffset = 100),
        IrFileSymbolImpl(), FqName("org.sample"), module,
    ).also { module.files += it }
    private val intType = IrSimpleTypeImpl(
        IrClassSymbolImpl(signature = IdSignature.CommonSignature("kotlin", "Int", null, 0L, description = null)),
        hasQuestionMark = false,
        arguments = emptyList(),
        annotations = emptyList(),
    )

    @Test
    fun `inline body changes propagate through a chain`() {
        val first = function("first")
        val second = function("second", call(first))
        val third = function("third", call(second))
        val oldHashes = hashes()

        first.setBody(constant(2))

        val newHashes = hashes()
        for (function in listOf(first, second, third)) {
            val symbol = InlineFunctionSymbol("org.sample", function.name.asString())
            assertNotEquals(oldHashes.getValue(symbol), newHashes.getValue(symbol))
        }
    }

    @Test
    fun `ordinary callee body changes do not change inline callers`() {
        val callee = function("callee", inline = false)
        function("caller", call(callee))
        val oldHashes = hashes()

        callee.setBody(constant(2))

        assertEquals(oldHashes, hashes())
    }

    @Test
    fun `default arguments include transitive inline bodies`() {
        val callee = function("callee")
        val caller = function("caller")
        val parameter = caller.addValueParameter("value", intType)
        parameter.defaultValue = IrFactoryImpl.createExpressionBody(call(callee))
        val symbol = InlineFunctionSymbol("org.sample", "caller")
        val oldHash = hashes().getValue(symbol)

        callee.setBody(constant(2))
        val changedCalleeHash = hashes().getValue(symbol)
        assertNotEquals(oldHash, changedCalleeHash)

        parameter.defaultValue = IrFactoryImpl.createExpressionBody(constant(3))
        assertNotEquals(changedCalleeHash, hashes().getValue(symbol))
    }

    @Test
    fun `overload order and unrelated declarations do not change hashes`() {
        function("overloaded")
        function("overloaded", constant(2)).addValueParameter("value", intType)
        val oldHashes = hashes()

        file.declarations.reverse()
        function("unrelated", inline = false)

        assertEquals(oldHashes, hashes())
    }

    @Test
    fun `source offsets and generated temporary names do not change hashes`() {
        val function = function("caller")
        val temporary = buildVariable(
            function, 0, 10, IrDeclarationOrigin.IR_TEMPORARY_VARIABLE, Name.identifier("tmp_17"), intType,
        ).apply { initializer = constant(1) }
        function.body = IrFactoryImpl.createBlockBody(
            0, 10, listOf(temporary, IrGetValueImpl(0, 10, temporary.symbol))
        )
        val oldHashes = hashes()

        temporary.name = Name.identifier("tmp_93")
        temporary.startOffset = 20
        temporary.endOffset = 30
        function.startOffset = 20
        function.endOffset = 40

        assertEquals(oldHashes, hashes())
    }

    @Test
    fun `inline accessors use the property lookup name and class scope`() {
        val holder = IrFactoryImpl.buildClass { name = Name.identifier("Holder") }.apply { parent = file }
        file.declarations += holder
        val property = IrFactoryImpl.buildProperty { name = Name.identifier("answer") }.apply { parent = holder }
        holder.declarations += property
        val getter = property.addGetter {
            returnType = intType
            isInline = true
        }
        getter.setBody(constant(1))

        val oldHashes = hashes()
        assertEquals(setOf(InlineFunctionSymbol("org.sample.Holder", "answer")), oldHashes.keys)

        getter.setBody(constant(2))
        assertNotEquals(oldHashes, hashes())
    }

    @Test
    fun `inline dependency cycles terminate and retain body changes`() {
        val first = function("first")
        val second = function("second", call(first))
        first.setBody(call(second))
        val oldHashes = hashes()

        second.body = IrFactoryImpl.createBlockBody(0, 10, listOf(constant(2), call(first)))

        assertNotEquals(oldHashes, hashes())
    }

    @Test
    fun `referenced constant values affect inline hashes`() {
        val property = constantProperty("constant", 1)
        function("caller", call(property.getter!!))
        val oldHashes = hashes()

        property.backingField!!.initializer = IrFactoryImpl.createExpressionBody(constant(2))

        assertNotEquals(oldHashes, hashes())
    }

    @Test
    fun `swapping referenced constant values changes the caller hash`() {
        val first = constantProperty("first", 1)
        val second = constantProperty("second", 2)
        val caller = function("caller")
        caller.body = IrFactoryImpl.createBlockBody(0, 10, listOf(call(first.getter!!), call(second.getter!!)))
        val oldHash = hashes().getValue(InlineFunctionSymbol("org.sample", "caller"))

        first.backingField!!.initializer = IrFactoryImpl.createExpressionBody(constant(2))
        second.backingField!!.initializer = IrFactoryImpl.createExpressionBody(constant(1))

        assertNotEquals(oldHash, hashes().getValue(InlineFunctionSymbol("org.sample", "caller")))
    }

    @Test
    fun `swapping same named inline bodies from different packages changes the caller hash`() {
        val first = function("sameName", constant(1))
        val second = function("sameName", constant(2))
        val otherFile = IrFileImpl(
            NaiveSourceBasedFileEntryImpl("other.kt", intArrayOf(0), maxOffset = 100),
            IrFileSymbolImpl(), FqName("org.other"), module,
        ).also { module.files += it }
        file.declarations.remove(second)
        second.parent = otherFile
        otherFile.declarations += second
        val caller = function("caller")
        caller.body = IrFactoryImpl.createBlockBody(0, 10, listOf(call(first), call(second)))
        val oldHash = hashes().getValue(InlineFunctionSymbol("org.sample", "caller"))

        first.setBody(constant(2))
        second.setBody(constant(1))

        assertNotEquals(oldHash, hashes().getValue(InlineFunctionSymbol("org.sample", "caller")))
    }

    @Test
    fun `swapping private inline bodies with the same name changes the transitive caller hash`() {
        val first = function("sameName", constant(1)).apply { visibility = DescriptorVisibilities.PRIVATE }
        val second = function("sameName", constant(2)).apply { visibility = DescriptorVisibilities.PRIVATE }
        val firstWrapper = function("firstWrapper", call(first))
        val secondWrapper = function("secondWrapper", call(second))
        val otherFile = IrFileImpl(
            NaiveSourceBasedFileEntryImpl("other/test.kt", intArrayOf(0), maxOffset = 100),
            IrFileSymbolImpl(), FqName("org.sample"), module,
        ).also { module.files += it }
        for (declaration in listOf(second, secondWrapper)) {
            file.declarations.remove(declaration)
            declaration.parent = otherFile
            otherFile.declarations += declaration
        }
        val caller = function("caller")
        caller.body = IrFactoryImpl.createBlockBody(0, 10, listOf(call(firstWrapper), call(secondWrapper)))
        val oldHash = hashes().getValue(InlineFunctionSymbol("org.sample", "caller"))

        first.setBody(constant(2))
        second.setBody(constant(1))

        assertNotEquals(oldHash, hashes().getValue(InlineFunctionSymbol("org.sample", "caller")))
    }

    @Test
    fun `relocating a source file does not change inline hashes`() {
        val privateFunction = function("privateFunction").apply { visibility = DescriptorVisibilities.PRIVATE }
        function("caller", call(privateFunction))
        val oldHashes = hashes()

        file.fileEntry = NaiveSourceBasedFileEntryImpl("/relocated/project/test.kt", intArrayOf(0), maxOffset = 100)

        assertEquals(oldHashes, hashes())
    }

    private fun constantProperty(name: String, value: Int): IrProperty {
        val property = IrFactoryImpl.buildProperty {
            this.name = Name.identifier(name)
            isConst = true
        }.apply { parent = file }
        file.declarations += property
        val field = IrFactoryImpl.buildField {
            this.name = property.name
            type = intType
        }.apply {
            parent = file
            correspondingPropertySymbol = property.symbol
            initializer = IrFactoryImpl.createExpressionBody(constant(value))
        }
        property.backingField = field
        property.addGetter { returnType = intType }
        return property
    }

    @Test
    fun `unbound references retain their identities`() {
        fun unboundCall(name: String) = IrCallImplWithShape(
            0, 10, intType,
            IrSimpleFunctionSymbolImpl(signature = IdSignature.CommonSignature("other", name, 1L, 0L, description = null)),
            typeArgumentsCount = 0,
            valueArgumentsCount = 0,
            contextParameterCount = 0,
            hasDispatchReceiver = false,
            hasExtensionReceiver = false,
        )

        val caller = function("caller", unboundCall("first"))
        val oldHashes = hashes()

        caller.setBody(unboundCall("second"))

        assertNotEquals(oldHashes, hashes())
    }

    private fun function(name: String, expression: IrExpression = constant(1), inline: Boolean = true): IrSimpleFunction =
        IrFactoryImpl.buildFun {
            this.name = Name.identifier(name)
            returnType = intType
            isInline = inline
        }.apply {
            parent = file
            setBody(expression)
            file.declarations += this
        }

    private fun IrSimpleFunction.setBody(expression: IrExpression) {
        body = IrFactoryImpl.createBlockBody(0, 10, listOf(expression))
    }

    private fun constant(value: Int) = IrConstImpl.int(0, 10, intType, value)

    private fun call(function: IrSimpleFunction) = IrCallImpl(0, 10, intType, function.symbol)

    private fun hashes() = collectInlineFunctionHashes(module).getValue(file)
}
