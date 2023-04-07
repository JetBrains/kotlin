/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.constant.ErrorValue
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator

interface IrInterpreterDumpHandler {
    val testServices: TestServices
    private val globalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    fun processModule(module: TestModule) {
        if (!module.isSuppressedForK2() && testServices.defaultsProvider.defaultFrontend == FrontendKinds.ClassicFrontend) {
            module.files.forEach { testFile ->
                val expected = globalMetadataInfoHandler.getExistingMetaInfosForFile(testFile)
                globalMetadataInfoHandler.addMetadataInfosForFile(testFile, expected)
            }
            return
        }

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val evaluatedConstTracker = configuration.get(CommonConfigurationKeys.EVALUATED_CONST_TRACKER) ?: return
        val irModule = testServices.dependencyProvider.getArtifact(module, BackendKinds.IrBackend).irModuleFragment
        for ((irFile, testFile) in matchIrFileWithTestFile(irModule, module)) {
            evaluatedConstTracker.processFile(testFile, irFile, testServices.defaultsProvider.defaultTargetBackend ?: module.targetBackend)
        }
    }

    fun TestModule.isSuppressedForK2(): Boolean {
        val ignoredBackends = this.directives[IGNORE_BACKEND_K2]
        val targetBackend = testServices.defaultsProvider.defaultTargetBackend ?: this.targetBackend
        return targetBackend in ignoredBackends || TargetBackend.ANY in ignoredBackends
    }

    private fun EvaluatedConstTracker.processFile(testFile: TestFile, irFile: IrFile, targetBackend: TargetBackend?) {
        irFile.accept(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitDeclaration(declaration: IrDeclarationBase) {
                if (declaration.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS) return
                visitAnnotations(declaration)
                super.visitDeclaration(declaration)
            }

            // TODO can be dropped if we are going to draw one info per line segment
            override fun visitField(declaration: IrField) {
                if (targetBackend == TargetBackend.JVM_IR) {
                    declaration.correspondingPropertySymbol?.owner?.let { visitAnnotations(it) }
                }
                super.visitField(declaration)
            }

            private fun visitAnnotations(annotationContainer: IrAnnotationContainer) {
                annotationContainer.annotations.forEach { annotation ->
                    annotation.acceptVoid(this)
                }
            }

            override fun visitConst(expression: IrConst<*>) {
                val constantValue = this@processFile.load(expression.startOffset, expression.endOffset) ?: return
                val message = constantValue.stringTemplateValue()
                val metaInfo = ParsedCodeMetaInfo(
                    expression.startOffset, expression.endOffset,
                    attributes = mutableListOf(),
                    tag = if (constantValue is ErrorValue) "WAS_NOT_EVALUATED" else "EVALUATED",
                    description = StringUtil.escapeLineBreak(message)
                )
                globalMetadataInfoHandler.addMetadataInfosForFile(testFile, listOf(metaInfo))
            }
        }, null)
    }
}

class JvmIrInterpreterDumpHandler(testServices: TestServices) : IrInterpreterDumpHandler, JvmBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        processModule(module)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}

class JsIrInterpreterDumpHandler(testServices: TestServices) : IrInterpreterDumpHandler, JsBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {
        processModule(module)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}

class KlibInterpreterDumpHandler(testServices: TestServices) : IrInterpreterDumpHandler, KlibArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) {
        if (JsEnvironmentConfigurator.isMainModule(module, testServices)) return
        processModule(module)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}

