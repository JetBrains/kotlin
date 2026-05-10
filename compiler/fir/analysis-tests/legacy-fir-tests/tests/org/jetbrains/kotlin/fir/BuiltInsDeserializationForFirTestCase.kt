/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmConfigurationPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.staticScope
import org.jetbrains.kotlin.fir.java.deserialization.FirJvmBuiltinsSymbolProvider
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.processAllCallables
import org.jetbrains.kotlin.fir.scopes.processAllClassifiers
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.TestDataAssertions
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.util.PerformanceManagerImpl
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.jupiter.api.Test
import java.io.File

class BuiltInsDeserializationForFirTestCase {
    companion object {
        private val BUILTIN_PACKAGE_NAMES = listOf(
            StandardNames.BUILT_INS_PACKAGE_FQ_NAME,
            StandardNames.COLLECTIONS_PACKAGE_FQ_NAME,
            StandardNames.RANGES_PACKAGE_FQ_NAME
        )

        private const val TEST_DATA_PATH = "compiler/fir/analysis-tests/legacy-fir-tests/testData/builtIns"
    }

    // ------------------------ actual tests ------------------------

    @Test
    fun testFallbackBuiltInsModifiedJdk() {
        doTest("fallbackBuiltIns_modifiedMockJDK.txt", TestJdkKind.MODIFIED_MOCK_JDK)
    }

    @Test
    fun testFallbackBuiltInsFullJdk8() {
        doTest("fallbackBuiltIns_fullJDK8.txt", TestJdkKind.FULL_JDK_8)
    }

    @Test
    fun testFallbackBuiltInsFullJdk21() {
        doTest("fallbackBuiltIns_fullJDK21.txt", TestJdkKind.FULL_JDK_21)
    }

    // ------------------------ test utilities ------------------------

    private fun doTest(expectedFileName: String, jdkKind: TestJdkKind) {
        val disposable = Disposer.newDisposable()
        try {
            val session = createSession(disposable, jdkKind)
            val builder = StringBuilder()
            for (packageFqName in BUILTIN_PACKAGE_NAMES) {
                dumpPackageContent(session, packageFqName, builder)
            }
            val expectedFile = ForTestCompileRuntime.transformTestDataPath("$TEST_DATA_PATH/$expectedFileName")
            TestDataAssertions.assertEqualsToFile(
                expectedFile,
                builder.toString().trimEnd() + "\n"
            )
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private fun createSession(
        rootDisposable: Disposable,
        jdkKind: TestJdkKind,
    ): FirSession {
        val emptyInput = ArgumentsPipelineArtifact(
            arguments = K2JVMCompilerArguments().apply {
                noStdlib = true
                noReflect = true
                noJdk = true
                allowNoSourceFiles = true
                val jdk = when (jdkKind) {
                    TestJdkKind.MOCK_JDK -> KtTestUtil.findMockJdkRtJar()
                    TestJdkKind.MODIFIED_MOCK_JDK -> KtTestUtil.findMockJdkRtModified()
                    TestJdkKind.FULL_JDK_8 -> KtTestUtil.getJdk8Home()
                    TestJdkKind.FULL_JDK_11 -> KtTestUtil.getJdk11Home()
                    TestJdkKind.FULL_JDK_17 -> KtTestUtil.getJdk17Home()
                    TestJdkKind.FULL_JDK_21 -> KtTestUtil.getJdk21Home()
                    TestJdkKind.FULL_JDK -> File(System.getProperty("java.home"))
                }
                classpath = jdk.absolutePath
            },
            services = Services.EMPTY,
            rootDisposable,
            GroupingMessageCollector(MessageCollector.NONE, false, false),
            PerformanceManagerImpl(JvmPlatforms.defaultJvmPlatform, "stub for builtins test"),
        )
        val configurationOutput = JvmConfigurationPipelinePhase.executePhase(emptyInput)
        val frontendOutput = JvmFrontendPipelinePhase.executePhase(configurationOutput)!!
        return frontendOutput.frontendOutput.outputs.first().session
    }

    private fun dumpPackageContent(
        session: FirSession,
        packageFqName: FqName,
        builder: StringBuilder,
    ) {
        val scopeSession = ScopeSession()
        val symbolProvider =
            (session.symbolProvider as FirCachingCompositeSymbolProvider).providers.firstIsInstance<FirJvmBuiltinsSymbolProvider>()
        val namesProvider = symbolProvider.symbolNamesProvider
        val classifierNames = namesProvider.getTopLevelClassifierNamesInPackage(packageFqName).orEmpty()
        val callableNames = namesProvider.getTopLevelCallableNamesInPackage(packageFqName).orEmpty()

        val decoration = "=".repeat(20 + packageFqName.toString().length + 11)
        builder.appendLine(decoration)
        builder.appendLine("========== Package: $packageFqName ==========")
        builder.appendLine(decoration)
        builder.appendLine()

        val firRenderer = FirRenderer(builder, classMemberRenderer = null)

        for (name in callableNames) {
            for (symbol in symbolProvider.getTopLevelCallableSymbols(packageFqName, name)) {
                @OptIn(SymbolInternals::class)
                firRenderer.renderElementAsString(symbol.fir)
                builder.appendLine()
            }
        }

        for (name in classifierNames) {
            val classSymbol = symbolProvider.getClassLikeSymbolByClassId(ClassId.topLevel(packageFqName.child(name))) ?: continue
            context(session, scopeSession) {
                renderClass(firRenderer, classSymbol)
                builder.appendLine()
            }
        }
    }

    @OptIn(SymbolInternals::class)
    context(session: FirSession, scopeSession: ScopeSession)
    private fun renderClass(renderer: FirRenderer, classSymbol: FirRegularClassSymbol) {
        val printer = renderer.printer

        val scope = classSymbol.unsubstitutedScope(
            session,
            scopeSession,
            withForcedTypeCalculator = true,
            memberRequiredPhase = FirResolvePhase.STATUS,
        )
        renderer.renderElementAsString(classSymbol.fir)
        printer.renderInBraces {
            scope.processAllCallables {
                renderer.renderElementAsString(it.fir)
                printer.println()
            }

            scope.processDeclaredConstructors {
                renderer.renderElementAsString(it.fir)
                printer.println()
            }

            scope.processAllClassifiers {
                if (it is FirRegularClassSymbol) {
                    renderClass(renderer, it)
                    printer.println()
                }
            }

            classSymbol.staticScope(session, scopeSession)?.processAllCallables {
                renderer.renderElementAsString(it.fir)
                printer.println()
            }
        }
    }
}
