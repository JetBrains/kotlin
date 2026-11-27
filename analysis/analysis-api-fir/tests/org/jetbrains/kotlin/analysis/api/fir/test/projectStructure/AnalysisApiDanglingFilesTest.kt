/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.projectStructure

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.platform.modification.publishGlobalModuleStateModificationEvent
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.TRANSPARENT_COPY_ORIGIN_KEY
import org.jetbrains.kotlin.analysis.api.projectStructure.copyOrigin
import org.jetbrains.kotlin.analysis.api.projectStructure.danglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.projectStructure.isStable
import org.jetbrains.kotlin.analysis.api.projectStructure.isTransparentCopyOrigin
import org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulSymbol
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.analyzeCopy
import org.jetbrains.kotlin.analysis.api.session.canBeAnalysed
import org.jetbrains.kotlin.analysis.api.session.useSiteModule
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.api.types.builtinTypes
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.reflect.full.isSubclassOf
import kotlin.time.Duration.Companion.seconds

/**
 * Custom Analysis API tests covering dangling files, including code fragments.
 */
class AnalysisApiDanglingFilesTest : AbstractAnalysisApiExecutionTest("testData/projectStructure/danglingFiles") {
    override val configurator = LLSourceLikeTestConfigurator()

    /**
     * See KT-79109.
     */
    @Test
    fun codeFragmentCopy(mainFile: KtFile, testServices: TestServices) {
        val assertions = testServices.assertions

        val simpleClass = mainFile.declarations.single() as KtClass
        val method = simpleClass.declarations.first() as KtNamedFunction
        assertions.assertEquals("method", method.name)

        val context = simpleClass.declarations.last() as KtNamedFunction
        assertions.assertEquals("context", context.name)

        val codeFragment = KtPsiFactory(mainFile.project).createExpressionCodeFragment("method()", context)
        val codeFragmentExpression = codeFragment.getContentElement() as KtCallExpression

        val codeFragmentCopy = codeFragment.copy() as KtExpressionCodeFragment
        val codeFragmentCopyExpression = codeFragmentCopy.getContentElement() as KtCallExpression

        analyze(codeFragmentExpression) {
            assertions.assertTrue(useSiteModule::class.isSubclassOf(KaDanglingFileModule::class))

            val callableSymbol = codeFragmentExpression.resolveSuccessfulSymbol()

            // Check whether the code fragment is configured correctly
            assertions.assertEquals(method, callableSymbol?.psi)

            // Check whether elements from the original file are analyzable
            assertions.assertTrue(simpleClass.canBeAnalysed())

            // Elements from the fragment copy are not expected to be analyzable in the context of original
            // since each code fragment has its own module
            assertions.assertFalse(codeFragmentCopyExpression.canBeAnalysed())
        }

        analyze(codeFragmentCopyExpression) {
            assertions.assertTrue(useSiteModule::class.isSubclassOf(KaDanglingFileModule::class))

            val callableSymbol = codeFragmentCopyExpression.resolveSuccessfulSymbol()

            // Check whether the code fragment copy is configured correctly
            assertions.assertEquals(method, callableSymbol?.psi)

            // Check whether elements from the original file are analyzable
            assertions.assertTrue(simpleClass.canBeAnalysed())

            // Elements from the original fragment should be analyzable in the context of copy
            assertions.assertTrue(codeFragmentExpression.canBeAnalysed())
        }
    }

    /**
     * The test covers a situation where a physical code fragment depends on a non-physical dangling file. The dangling file of the code
     * fragment is expected to be unstable since it depends on an unstable context module.
     *
     * The test ensures that we can correctly create and work with such `unstable --> unstable` LL FIR sessions (see KT-84701).
     */
    @Test
    fun codeFragmentsWithUnstableContext(mainFile: KtFile, testServices: TestServices) {
        val assertions = testServices.assertions

        // Disabled event system --> unstable dangling file.
        val ktPsiFactory = KtPsiFactory.contextual(mainFile, markGenerated = true, eventSystemEnabled = false)
        val danglingFile = ktPsiFactory.createFile("fake.kt", mainFile.text)

        analyze(danglingFile) {
            val useSiteModule = useSiteModule as? KaDanglingFileModule
                ?: error("Expected the use-site module to be a dangling file module")

            assertions.assertFalse(useSiteModule.isStable) { "Expected the dangling file to be unstable." }
        }

        val simpleClass = danglingFile.declarations.single() as KtClass
        val method = simpleClass.declarations.first() as KtNamedFunction
        assertions.assertEquals("method", method.name)

        val context = simpleClass.declarations.last() as KtNamedFunction
        assertions.assertEquals("context", context.name)

        val codeFragment = KtPsiFactory(mainFile.project).createExpressionCodeFragment("method()", context)
        val codeFragmentExpression = codeFragment.getContentElement() as KtCallExpression

        analyze(codeFragmentExpression) {
            val useSiteModule = useSiteModule as? KaDanglingFileModule
                ?: error("Expected the use-site module to be a dangling file module")

            // Even though the code fragment is most definitely physical, it should still be unstable because its context module is
            // unstable.
            assertions.assertFalse(useSiteModule.isStable) { "Expected the code fragment to be unstable." }

            val callableSymbol = codeFragmentExpression.resolveSuccessfulSymbol()

            // Check whether the code fragment is configured correctly.
            assertions.assertEquals(method, callableSymbol?.psi)

            // Check whether elements from the dangling file are analyzable.
            assertions.assertTrue(simpleClass.canBeAnalysed())
        }

        // Last, we also check a physical code fragment depending on the above code fragment. To illustrate, we have the following
        // constellation:
        //
        // physical --> physical --> non-physical
        //
        // To ensure that `isStable` is correctly (recursively) calculated, we should check this case explicitly.
        val nestedCodeFragment = KtPsiFactory(mainFile.project).createExpressionCodeFragment("method()", codeFragmentExpression)
        val nestedCodeFragmentExpression = nestedCodeFragment.getContentElement() as KtCallExpression

        analyze(nestedCodeFragmentExpression) {
            val useSiteModule = useSiteModule as? KaDanglingFileModule
                ?: error("Expected the use-site module to be a dangling file module")

            assertions.assertFalse(useSiteModule.isStable) { "Expected the nested code fragment to be unstable." }

            val callableSymbol = nestedCodeFragmentExpression.resolveSuccessfulSymbol()

            // Check whether the code fragment is configured correctly.
            assertions.assertEquals(method, callableSymbol?.psi)

            // Check whether elements from the dangling file are analyzable.
            assertions.assertTrue(simpleClass.canBeAnalysed())

            // Elements from the original fragment should be analyzable in the context of copy.
            assertions.assertTrue(codeFragmentExpression.canBeAnalysed())
        }

        analyze(codeFragmentExpression) {
            // Elements from the nested code fragment are not expected to be analyzable in the context of the original code fragment,
            // since each code fragment has its own module.
            assertions.assertFalse(nestedCodeFragmentExpression.canBeAnalysed())
        }

        // To test the situation where nested creation leads to a recursive update exception (see KT-84701), we have to reset session caches
        // and request the outermost session right away. (The order of analysis matters for session creation: If the inner sessions are
        // created one by one on `analyze`, when an outer session requires its context module session, it will already exist in the cache.)
        runWriteAction {
            mainFile.project.publishGlobalModuleStateModificationEvent()
        }

        analyze(nestedCodeFragmentExpression) {
            assertions.assertTrue(useSiteModule::class.isSubclassOf(KaDanglingFileModule::class))
        }
    }

    /**
     * This test ensures that the LL FIR session cache correctly cleans up invalid unstable dangling file sessions.
     */
    @Test
    fun invalidUnstableDanglingFileSession(mainFile: KtFile, testServices: TestServices) {
        val assertions = testServices.assertions

        // Disabled event system --> unstable dangling file.
        val ktPsiFactory = KtPsiFactory.contextual(mainFile, markGenerated = true, eventSystemEnabled = false)
        val danglingFile = ktPsiFactory.createFile("fake.kt", mainFile.text)

        val simpleClass = danglingFile.declarations.single() as KtClass
        val method = simpleClass.declarations.first() as KtNamedFunction
        assertions.assertEquals("method", method.name)

        // Calling `analyze` ensures that the cache creates a dangling file session for the file.
        analyze(danglingFile) {
            assertions.assertEquals(builtinTypes.int, method.symbol.returnType)
        }

        // This invalidates the dangling file session, so the next `analyze` call must create a new unstable dangling file session.
        danglingFile.clearCaches()

        // There are several things which can go wrong in the session cache which would cause the test to fail or stall. Chiefly, we can
        // get an exception from the session cache because the session is invalid, or we could run into an infinite loop during updating
        // against which we have to guard the test.
        //
        // The assertion acts only as a smoke test to ensure that the resulting session can run lazy resolution correctly.
        assertions.assertTimeoutPreemptively(
            10.seconds,
            {
                "Could not get the analysis session for the dangling file within 10 seconds." +
                        " The session cache has likely run into an infinite loop."
            },
        ) {
            analyze(danglingFile) {
                assertions.assertEquals(builtinTypes.int, method.symbol.returnType)
            }
        }
    }

    /**
     * An in-memory file copy created by [KtPsiFactory] has no [virtualFile][com.intellij.psi.PsiFile.getVirtualFile]. It should still be
     * analyzable in the [PREFER_SELF][KaDanglingFileResolutionMode.PREFER_SELF] mode, both when the mode is set explicitly and when it is
     * calculated automatically, with the copy's own declarations taking precedence over the original ones (see KT-68102).
     */
    @Test
    fun inMemoryFileCopyPreferSelf(mainFile: KtFile, testServices: TestServices) {
        val assertions = testServices.assertions

        // Changing the inferred return type of `method` is an out-of-block modification.
        val copyText = mainFile.text.replace("fun method() = 5", "fun method() = \"five\"")
        val fileCopy = KtPsiFactory.contextual(mainFile).createFile("copy.kt", copyText)
        fileCopy.originalFile = mainFile
        assertions.assertTrue(fileCopy.virtualFile == null) { "Expected an in-memory file copy without a virtual file." }

        analyzeCopy(fileCopy, KaDanglingFileResolutionMode.PREFER_SELF) {
            checkInMemoryFileCopyPreferSelf(fileCopy, testServices)
        }

        analyze(fileCopy) {
            checkInMemoryFileCopyPreferSelf(fileCopy, testServices)
        }
    }

    context(_: KaSession)
    private fun checkInMemoryFileCopyPreferSelf(fileCopy: KtFile, testServices: TestServices) {
        val assertions = testServices.assertions

        val useSiteModule = useSiteModule as? KaDanglingFileModule
            ?: error("Expected the use-site module to be a dangling file module")

        assertions.assertEquals(KaDanglingFileResolutionMode.PREFER_SELF, useSiteModule.resolutionMode)
        assertions.assertEquals(listOf(fileCopy), useSiteModule.files)

        val simpleClass = fileCopy.declarations.single() as KtClass
        val method = simpleClass.declarations.first() as KtNamedFunction
        assertions.assertEquals("method", method.name)

        val usage = simpleClass.declarations.last() as KtNamedFunction
        assertions.assertEquals("usage", usage.name)

        val call = usage.bodyExpression as KtCallExpression

        assertions.assertTrue(fileCopy.canBeAnalysed())
        assertions.assertTrue(simpleClass.canBeAnalysed())
        assertions.assertTrue(call.canBeAnalysed())

        // The call should resolve to the declaration from the copy, not from the original file.
        assertions.assertEquals(method, call.resolveSuccessfulSymbol()?.realPsi)
        assertions.assertEquals(builtinTypes.string, method.symbol.returnType)
        assertions.assertEquals(builtinTypes.string, usage.symbol.returnType)
    }

    /**
     * A copy of a [transparent copy origin][isTransparentCopyOrigin] should be analyzed as an independent dangling file in the context of
     * the origin: it sees its own declarations instead of the ones from the origin, and it has the same access to the context file as the
     * origin. The same applies to a copy of such a copy when the copy is a transparent copy origin itself.
     */
    @Test
    fun transparentCopyOrigin(mainFile: KtFile, testServices: TestServices) {
        val assertions = testServices.assertions

        // With the event system enabled, the dangling file becomes the original file of its copies.
        val ktPsiFactory = KtPsiFactory.contextual(mainFile, markGenerated = true, eventSystemEnabled = true)
        val danglingFile = ktPsiFactory.createFile(
            "dangling.kt",
            """
                class DanglingClass {
                    fun method() = "five"

                    fun usage() = method()
                }

                fun privateFunctionUsage() = privateFunction()
            """.trimIndent(),
        )
        danglingFile.putUserData(TRANSPARENT_COPY_ORIGIN_KEY, true)

        val fileCopy = danglingFile.copy() as KtFile
        assertions.assertEquals(danglingFile, fileCopy.copyOrigin)

        // Only a copy with the event system enabled becomes the original file of its own copies. Being a transparent copy origin as
        // well, it lets them through to the context of the dangling file.
        val physicalFileCopy = ktPsiFactory.createFile("dangling.kt", danglingFile.text)
        physicalFileCopy.originalFile = danglingFile
        physicalFileCopy.putUserData(TRANSPARENT_COPY_ORIGIN_KEY, true)

        val fileCopyOfCopy = physicalFileCopy.copy() as KtFile
        assertions.assertEquals(physicalFileCopy, fileCopyOfCopy.copyOrigin)

        val contextModule = analyze(mainFile) { useSiteModule }
        val privateFunction = mainFile.declarations.single() as KtNamedFunction

        for (file in listOf(danglingFile, fileCopy, physicalFileCopy, fileCopyOfCopy)) {
            analyze(file) {
                checkFileWithTransparentCopyOrigin(file, contextModule, privateFunction, testServices)
            }
        }

        // The resolution mode is forced for the module only: no mode is explicitly set for the copy.
        assertions.assertEquals(null, fileCopy.danglingFileResolutionMode)

        // An explicitly requested resolution mode cannot make the copy depend on declarations from the origin.
        analyzeCopy(fileCopy, KaDanglingFileResolutionMode.IGNORE_SELF) {
            checkFileWithTransparentCopyOrigin(fileCopy, contextModule, privateFunction, testServices)
        }
    }

    context(_: KaSession)
    private fun checkFileWithTransparentCopyOrigin(
        file: KtFile,
        contextModule: KaModule,
        privateFunction: KtNamedFunction,
        testServices: TestServices,
    ) {
        val assertions = testServices.assertions

        val useSiteModule = useSiteModule as? KaDanglingFileModule
            ?: error("Expected the use-site module to be a dangling file module")

        assertions.assertEquals(KaDanglingFileResolutionMode.PREFER_SELF, useSiteModule.resolutionMode)
        assertions.assertEquals(contextModule, useSiteModule.contextModule)
        assertions.assertEquals(listOf(file), useSiteModule.files)

        val danglingClass = file.declarations.first() as KtClass
        val method = danglingClass.declarations.first() as KtNamedFunction
        assertions.assertEquals("method", method.name)

        val usage = danglingClass.declarations.last() as KtNamedFunction
        assertions.assertEquals("usage", usage.name)

        val privateFunctionUsage = file.declarations.last() as KtNamedFunction
        assertions.assertEquals("privateFunctionUsage", privateFunctionUsage.name)

        // The call should resolve to the declaration from the analyzed file.
        val methodCall = usage.bodyExpression as KtCallExpression
        assertions.assertEquals(method, methodCall.resolveSuccessfulSymbol()?.realPsi)
        assertions.assertEquals(builtinTypes.string, usage.symbol.returnType)

        // Private declarations of the context file should be visible.
        val privateFunctionCall = privateFunctionUsage.bodyExpression as KtCallExpression
        assertions.assertEquals(privateFunction, privateFunctionCall.resolveSuccessfulSymbol()?.realPsi)
        assertions.assertEquals(builtinTypes.int, privateFunctionUsage.symbol.returnType)
    }

    /**
     * A code fragment cannot be a [transparent copy origin][isTransparentCopyOrigin]: the [TRANSPARENT_COPY_ORIGIN_KEY] set for it should
     * be reported instead of being silently ignored.
     */
    @Test
    @TestMetadata("transparentCopyOrigin")
    fun transparentCopyOriginCodeFragment(mainFile: KtFile, testServices: TestServices) {
        val codeFragment = KtPsiFactory(mainFile.project).createExpressionCodeFragment("privateFunction()", mainFile)
        testServices.assertions.assertFalse(codeFragment.isTransparentCopyOrigin)

        codeFragment.putUserData(TRANSPARENT_COPY_ORIGIN_KEY, true)
        assertThrows<IllegalStateException> { codeFragment.isTransparentCopyOrigin }
    }
}
