/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.TestIrBuiltins
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for detecting signature clashes in custom-built IR.
 *
 * These tests build IR programmatically and then use
 * the signature computation and clash detection infrastructure to verify that
 * signature clashes are properly detected.
 */
abstract class IrSignatureClashTest {
    abstract val irMangler: KotlinMangler.IrMangler

    private lateinit var module: IrModuleFragment
    private lateinit var globalDeclarationTable: GlobalDeclarationTable
    private lateinit var declarationTable: DeclarationTable.Default

    @BeforeEach
    fun setUp() {
        val moduleDescriptor = ModuleDescriptorImpl(
            Name.special("<testModule>"),
            LockBasedStorageManager("IrSignatureClashTest"),
            DefaultBuiltIns.Instance
        )
        module = IrModuleFragmentImpl(moduleDescriptor)
        globalDeclarationTable = object : GlobalDeclarationTable(irMangler) {}
        declarationTable = DeclarationTable.Default(globalDeclarationTable)
    }

    @Test
    fun `signature clashes are detected correctly`() {
        val file = createIrFile()

        val func1 = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
            startOffset = 0
            endOffset = 10
        }.apply { parent = file }

        val func2 = IrFactoryImpl.buildFun {
            name = Name.identifier("foo")
            returnType = TestIrBuiltins.unitType
            startOffset = 20
            endOffset = 30
        }.apply { parent = file }

        file.declarations += func1
        file.declarations += func2

        declarationTable.inFile(file) {
            trackDeclaration(func1)
            trackDeclaration(func2)
        }

        assertClashes()
    }

    /**
     * Verifies that companion blocks do not cause signature clashes with the following code:
     *
     * class MyClass {
     *     fun memberFunction() {}
     *     companion {
     *         fun memberFunction() {}
     *     }
     * }
     */
    @Test
    fun `function member of a class without dispatch receiver does not clash with corresponding function with dispatch receiver`() {
        val file = createIrFile()

        val klass = IrFactoryImpl.buildClass {
            name = Name.identifier("MyClass")
        }.apply {
            parent = file
            createThisReceiverParameter()
        }

        val memberFunctionWithoutDispatchReceiver = IrFactoryImpl.buildFun {
            name = Name.identifier("memberFunction")
            returnType = TestIrBuiltins.unitType
            startOffset = 0
            endOffset = 10
        }.apply { parent = klass }

        val memberFunctionWithDispatchReceiver = IrFactoryImpl.buildFun {
            name = Name.identifier("memberFunction")
            returnType = TestIrBuiltins.unitType
            startOffset = 20
            endOffset = 30
        }.apply {
            parent = klass
            addValueParameter {
                name = SpecialNames.THIS
                type = klass.defaultType
            }.apply { kind = IrParameterKind.DispatchReceiver }
        }

        file.declarations += klass
        klass.declarations += memberFunctionWithoutDispatchReceiver
        klass.declarations += memberFunctionWithDispatchReceiver

        declarationTable.inFile(file) {
            trackDeclaration(memberFunctionWithoutDispatchReceiver)
            trackDeclaration(memberFunctionWithDispatchReceiver)
        }

        assertNoClashes()
    }

    /**
     * Verifies that companion blocks do not cause signature clashes with the following code:
     *
     * class MyClass {
     *     val memberProperty get() = 42
     *     companion {
     *         val memberProperty get() = 42
     *     }
     * }
     */
    @Test
    fun `property member of a class does not clash with accessors for that property without dispatch receiver`() {
        val file = createIrFile()

        val klass = IrFactoryImpl.buildClass {
            name = Name.identifier("MyClass")
        }.apply {
            parent = file
            createThisReceiverParameter()
        }

        val memberProperty = IrFactoryImpl.buildProperty {
            name = Name.identifier("memberProperty")
            startOffset = 20
            endOffset = 30
        }.apply {
            parent = klass
            getter = IrFactoryImpl.buildFun {
                name = Name.special("<get-memberProperty>")
                returnType = TestIrBuiltins.intType
                startOffset = 20
                endOffset = 30
            }.also {
                it.correspondingPropertySymbol = this.symbol
                it.parent = klass
                it.addValueParameter {
                    name = SpecialNames.THIS
                    type = klass.defaultType
                }.apply { kind = IrParameterKind.DispatchReceiver }
            }
            setter = IrFactoryImpl.buildFun {
                name = Name.special("<set-memberProperty>")
                returnType = TestIrBuiltins.unitType
                startOffset = 20
                endOffset = 30
            }.also {
                it.correspondingPropertySymbol = this.symbol
                it.parent = klass
                it.addValueParameter {
                    name = SpecialNames.THIS
                    type = klass.defaultType
                }.apply { kind = IrParameterKind.DispatchReceiver }
                it.addValueParameter {
                    name = Name.identifier("value")
                    type = TestIrBuiltins.intType
                }
            }
        }

        val memberPropertyWithoutDispatchReceiver = IrFactoryImpl.buildProperty {
            name = Name.identifier("memberProperty")
            startOffset = 20
            endOffset = 30
        }.apply {
            parent = klass
            getter = IrFactoryImpl.buildFun {
                name = Name.special("<get-memberProperty>")
                returnType = TestIrBuiltins.intType
                startOffset = 20
                endOffset = 30
            }.also {
                it.correspondingPropertySymbol = this.symbol
                it.parent = klass
            }
            setter = IrFactoryImpl.buildFun {
                name = Name.special("<set-memberProperty>")
                returnType = TestIrBuiltins.unitType
                startOffset = 20
                endOffset = 30
            }.also {
                it.correspondingPropertySymbol = this.symbol
                it.parent = klass
                it.addValueParameter {
                    name = Name.identifier("value")
                    type = TestIrBuiltins.intType
                }
            }
        }

        file.declarations += klass
        klass.declarations += memberProperty
        klass.declarations += memberPropertyWithoutDispatchReceiver

        declarationTable.inFile(file) {
            trackDeclaration(memberProperty)
            trackDeclaration(memberProperty.getter!!)
            trackDeclaration(memberProperty.setter!!)
            trackDeclaration(memberPropertyWithoutDispatchReceiver)
            trackDeclaration(memberPropertyWithoutDispatchReceiver.getter!!)
            trackDeclaration(memberPropertyWithoutDispatchReceiver.setter!!)
        }

        assertNoClashes()
    }

    private fun createIrFile(
        name: String = "test.kt",
        packageFqName: FqName = FqName("org.sample"),
    ): IrFile {
        val fileEntry = NaiveSourceBasedFileEntryImpl(name, lineStartOffsets = intArrayOf(0, 10, 25), maxOffset = 75)
        return IrFileImpl(fileEntry, IrFileSymbolImpl(), packageFqName).also { module.files += it }
    }

    private fun trackDeclaration(declaration: IrDeclaration): IdSignature =
        declarationTable.signatureByDeclaration(declaration, compatibleMode = true, recordInSignatureClashDetector = true)

    private fun createIrDiagnosticReporter(): IrDiagnosticReporter {
        val diagnosticReporter = object : DiagnosticReporter() {
            private var _hasErrors = false
            override val hasErrors: Boolean get() = _hasErrors

            override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
                if (diagnostic != null) {
                    _hasErrors = true
                }
            }
        }
        return KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter, LanguageVersionSettingsImpl.DEFAULT)
    }

    private fun assertNoClashes() {
        val irDiagnosticReporter = createIrDiagnosticReporter()
        globalDeclarationTable.clashDetector.reportErrorsTo(irDiagnosticReporter)
        assert(!irDiagnosticReporter.hasErrors)
    }

    private fun assertClashes() {
        val irDiagnosticReporter = createIrDiagnosticReporter()
        globalDeclarationTable.clashDetector.reportErrorsTo(irDiagnosticReporter)
        assert(irDiagnosticReporter.hasErrors)
    }
}
