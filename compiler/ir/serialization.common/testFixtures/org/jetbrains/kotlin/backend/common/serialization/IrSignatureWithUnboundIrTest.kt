/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.TestIrBuiltins
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

abstract class IrSignatureWithUnboundIrTest {
    abstract val irMangler: KotlinMangler.IrMangler

    private lateinit var signatureComputer: PublicIdSignatureComputer
    private lateinit var module: IrModuleFragment

    @BeforeEach
    fun setUp() {
        val moduleDescriptor = ModuleDescriptorImpl(
            Name.special("<testModule>"),
            LockBasedStorageManager("IrSignatureClashTest"),
            DefaultBuiltIns.Instance
        )
        module = IrModuleFragmentImpl(moduleDescriptor)
        signatureComputer = PublicIdSignatureComputer(irMangler)
    }

    @Test
    fun `signature is correct for unbound IR`() {
        val file = createIrFile()

        val type = unboundType("org.sample", "Class")

        val func = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
            startOffset = 0
            endOffset = 10
        }.apply { parent = file }

        func.addValueParameter(name = Name.identifier("x"), type = type)

        file.declarations += func

        val signature = signatureComputer.computeSignature(func)
        assertEquals("org.sample/foo|foo(org.sample.Class){}[0]", signature.toString())
    }

    @Test
    fun `signature is correct for unbound IR in root package`() {
        val file = createIrFile()

        val type = unboundType("", "Class")

        val func = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
            startOffset = 0
            endOffset = 10
        }.apply { parent = file }

        func.addValueParameter(name = Name.identifier("x"), type = type)

        file.declarations += func

        val signature = signatureComputer.computeSignature(func)
        assertEquals("org.sample/foo|foo(Class){}[0]", signature.toString())
    }

    private fun unboundType(packageFqName: String, declarationFqName: String): IrSimpleType {
        val classSignature = IdSignature.CommonSignature(
            packageFqName = packageFqName,
            declarationFqName = declarationFqName,
            id = null,
            mask = 0L,
            description = null
        )

        val unboundSymbol = IrClassSymbolImpl(
            descriptor = null,
            signature = classSignature,
        )

        val type = IrSimpleTypeImpl(
            classifier = unboundSymbol,
            hasQuestionMark = false,
            arguments = emptyList(),
            annotations = emptyList()
        )
        return type
    }

    private fun createIrFile(
        name: String = "test.kt",
        packageFqName: FqName = FqName("org.sample"),
    ): IrFile {
        val fileEntry = NaiveSourceBasedFileEntryImpl(name, lineStartOffsets = intArrayOf(0, 10, 25), maxOffset = 75)
        return IrFileImpl(fileEntry, IrFileSymbolImpl(), packageFqName, module).also { module.files += it }
    }
}
