/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.addIfNotNull

class JvmIrLinkageModeTest : CodegenTestCase() {
    override val backend: TargetBackend
        get() = TargetBackend.JVM_IR

    private var enableLinkageViaSignatures: Boolean? = null

    private var source = """
        package test
        
        class Class
        interface Interface
        sealed class Sealed<T>
        enum class E { ENTRY }
        
        fun function(s: String): Array<Int> {
            fun Boolean.local() {}
            return arrayOf(s.length)
        }
        typealias S = String
        var property: S? = "OK"
    """.trimIndent()

    fun testLinkageViaDescriptors() {
        enableLinkageViaSignatures = false
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY)
        loadText(source)
        generateAndCreateClassLoader(true)
    }

    fun testLinkageViaSignatures() {
        enableLinkageViaSignatures = true
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY)
        loadText(source)
        generateAndCreateClassLoader(true)
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)
        if (enableLinkageViaSignatures!!) {
            configuration.put(JVMConfigurationKeys.LINK_VIA_SIGNATURES, true)
        }
    }

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        val idSignatureShouldBePresent = environment.configuration.getBoolean(JVMConfigurationKeys.LINK_VIA_SIGNATURES)
        IrGenerationExtension.registerExtension(environment.project, LinkageTestIrExtension(idSignatureShouldBePresent))
        super.setupEnvironment(environment)
    }

    private class LinkageTestIrExtension(val idSignatureShouldBePresent: Boolean) : IrGenerationExtension {
        override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
            val file = moduleFragment.files.single()
            val signatures = mutableListOf<IdSignature>()
            file.acceptVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitDeclaration(declaration: IrDeclarationBase) {
                    super.visitDeclaration(declaration)
                    signatures.addIfNotNull(declaration.symbol.signature)
                }
            })
            val allSignatures = signatures.map { it.render().substringBefore("|") }.toSet()
            if (idSignatureShouldBePresent) {
                val message = allSignatures.sorted().joinToString("\n")
                assertTrue(message, "test/Class" in allSignatures)
                assertTrue(message, "test/Interface" in allSignatures)
                assertTrue(message, "test/Sealed" in allSignatures)
                assertTrue(message, "test/E" in allSignatures)
                assertTrue(message, "test/function" in allSignatures)
                assertTrue(message, "test/S" in allSignatures)
                assertTrue(message, "test/property" in allSignatures)
                assertTrue(message, "test/property.<get-property>" in allSignatures)
                assertTrue(message, "test/property.<set-property>" in allSignatures)
            } else {
                assertEmpty(allSignatures)
            }
        }
    }
}
