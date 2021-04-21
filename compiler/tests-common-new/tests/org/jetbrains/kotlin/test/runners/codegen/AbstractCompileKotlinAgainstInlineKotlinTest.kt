/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.util.allUnbound
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.classic.ClassicBackendInput
import org.jetbrains.kotlin.test.backend.classic.ClassicJvmBackendFacade
import org.jetbrains.kotlin.test.backend.handlers.AbstractIrHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_MULTI_MODULE
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2ClassicBackendConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.ModuleTransformerForTwoFilesBoxTests
import org.jetbrains.kotlin.test.services.TestServices


@OptIn(TestInfrastructureInternals::class)
abstract class AbstractCompileKotlinAgainstInlineKotlinTestBase<I : ResultingArtifact.BackendInput<I>>(
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, I>>
    abstract val backendFacade: Constructor<BackendFacade<I, BinaryArtifacts.Jvm>>

    override fun TestConfigurationBuilder.configuration() = configurationImpl()

    protected fun TestConfigurationBuilder.configurationImpl() {
        commonConfigurationForCodegenTest(
            FrontendKinds.ClassicFrontend,
            ::ClassicFrontendFacade,
            frontendToBackendConverter,
            backendFacade
        )
        useInlineHandlers()
        commonHandlersForBoxTest()
        useModuleStructureTransformers(
            ModuleTransformerForTwoFilesBoxTests()
        )
        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor.bind(IGNORE_BACKEND_MULTI_MODULE))
    }
}

open class AbstractCompileKotlinAgainstInlineKotlinTest :
    AbstractCompileKotlinAgainstInlineKotlinTestBase<ClassicBackendInput>(TargetBackend.JVM) {
    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, ClassicBackendInput>>
        get() = ::ClassicFrontend2ClassicBackendConverter

    override val backendFacade: Constructor<BackendFacade<ClassicBackendInput, BinaryArtifacts.Jvm>>
        get() = ::ClassicJvmBackendFacade
}

open class AbstractIrCompileKotlinAgainstInlineKotlinTest :
    AbstractCompileKotlinAgainstInlineKotlinTestBase<IrBackendInput>(TargetBackend.JVM_IR) {
    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>
        get() = ::JvmIrBackendFacade
}

open class AbstractIrSerializeCompileKotlinAgainstInlineKotlinTest : AbstractIrCompileKotlinAgainstInlineKotlinTest() {
    override fun TestConfigurationBuilder.configuration() {
        // call super
        configurationImpl()
        useConfigurators(::SerializeSetter)
        useBackendHandlers(::CheckInlineBodies)
    }

    private class SerializeSetter(testServices: TestServices) : EnvironmentConfigurator(testServices) {
        override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
            configuration.put(JVMConfigurationKeys.SERIALIZE_IR, true)
        }
    }

    private class CheckInlineBodies(testServices: TestServices) : AbstractIrHandler(testServices) {
        val declaredInlineFunctions = mutableSetOf<IrSimpleFunction>()

        override fun processModule(module: TestModule, info: IrBackendInput) {
            val irModule = info.backendInput.irModuleFragment
            irModule.acceptChildrenVoid(InlineFunctionsCollector())
            irModule.acceptChildrenVoid(InlineCallBodiesCheck())
            assertions.assertTrue(info.backendInput.symbolTable.allUnbound.isEmpty())
        }

        override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
            // TODO("Not yet implemented")
        }

        inner class InlineFunctionsCollector : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                if (declaration.isInline) declaredInlineFunctions.add(declaration)
                super.visitSimpleFunction(declaration)
            }
        }

        inner class InlineCallBodiesCheck : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) {
                val symbol = expression.symbol
                assertions.assertTrue(symbol.isBound)
                val callee = symbol.owner
                if (callee in declaredInlineFunctions) {
                    val trueCallee = (callee as IrSimpleFunction).resolveFakeOverride()!!
                    assertions.assertNotNull(trueCallee.body)
                }
                super.visitMemberAccess(expression)
            }
        }

    }
}