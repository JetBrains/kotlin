package org.jetbrains.kotlin.checkers

import com.intellij.codeInsight.problems.MockWolfTheProblemSolver
import com.intellij.codeInsight.problems.WolfTheProblemSolverImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.problems.ProblemListener
import com.intellij.problems.WolfTheProblemSolver
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.util.ThrowableRunnable
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.idea.codeInsight.AbstractOutOfBlockModificationTest
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils.checkForUnexpectedErrors
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.runAll
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractKotlinHighlightWolfPassTest : KotlinLightCodeInsightFixtureTestCase() {

    private val disposable = Disposer.newDisposable("wolfTheProblemSolverParentDisposable")
    private var wolfTheProblemSolver: MockWolfTheProblemSolver? = null

    override fun setUp() {
        super.setUp()
        wolfTheProblemSolver = prepareWolf(project, disposable)
    }

    override fun tearDown() {
        runAll(
            // TODO: [VD] HAS TO BE UNCOMMENTED with 211
            //ThrowableRunnable { wolfTheProblemSolver?.resetDelegate() },
            ThrowableRunnable { Disposer.dispose(disposable) },
            ThrowableRunnable { super.tearDown() },
        )
    }

    private fun prepareWolf(project: Project, parentDisposable: Disposable): MockWolfTheProblemSolver {
        val wolfTheProblemSolver = WolfTheProblemSolver.getInstance(project) as MockWolfTheProblemSolver

        // TODO: [VD] HAS TO BE UNCOMMENTED with 211
//        val theRealSolver = WolfTheProblemSolverImpl.createInstance(project)
//        wolfTheProblemSolver.setDelegate(theRealSolver)
//        Disposer.register(parentDisposable, (theRealSolver as Disposable))

        return wolfTheProblemSolver
    }

    open fun doTest(filePath: String) {
        myFixture.configureByFile(fileName())
        val ktFile = file as KtFile

        myFixture.doHighlighting()
        // have to analyze file before any change to support incremental analysis
        val diagnosticsProvider: (KtFile) -> Diagnostics = { it.analyzeWithAllCompilerChecks().bindingContext.diagnostics }
        checkForUnexpectedErrors(ktFile, diagnosticsProvider)
        val wolf = WolfTheProblemSolver.getInstance(project)
        val virtualFile = ktFile.virtualFile
        val initialWolfErrors = wolfErrors(myFixture)
        // TODO: [VD] HAS TO BE UNCOMMENTED with 211
        //TestCase.assertEquals(initialWolfErrors, wolf.isProblemFile(virtualFile) || wolf.hasSyntaxErrors(virtualFile))

        var problemsAppeared = 0
        var problemsChanged = 0
        var problemsDisappeared = 0

        project.messageBus.connect(disposable).subscribe(ProblemListener.TOPIC, object : ProblemListener {
            override fun problemsAppeared(file: VirtualFile) {
                problemsAppeared++
            }

            override fun problemsChanged(file: VirtualFile) {
                problemsChanged++
            }

            override fun problemsDisappeared(file: VirtualFile) {
                problemsDisappeared++
            }
        })
        myFixture.type(AbstractOutOfBlockModificationTest.stringToType(myFixture))
        PsiDocumentManager.getInstance(project).commitDocument(myFixture.getDocument(ktFile))
        myFixture.doHighlighting()

        // TODO: [VD] HAS TO BE UNCOMMENTED with 211
//        val hasWolfErrors = hasWolfErrors(myFixture)
//        assertEquals(hasWolfErrors, wolf.isProblemFile(virtualFile) || wolf.hasSyntaxErrors(virtualFile))
//        if (hasWolfErrors && !initialWolfErrors) {
//            TestCase.assertTrue(problemsAppeared > 0)
//        } else {
//            assertEquals(0, problemsAppeared)
//        }
//        assertEquals(0, problemsDisappeared)
    }

    companion object {
        private const val HAS_WOLF_ERRORS_DIRECTIVE = "HAS-WOLF-ERRORS:"

        fun hasWolfErrors(fixture: JavaCodeInsightTestFixture): Boolean = findBooleanDirective(fixture, HAS_WOLF_ERRORS_DIRECTIVE)

        private const val WOLF_ERRORS_DIRECTIVE = "WOLF-ERRORS:"

        fun wolfErrors(fixture: JavaCodeInsightTestFixture): Boolean = findBooleanDirective(fixture, WOLF_ERRORS_DIRECTIVE)

        private fun findBooleanDirective(
            fixture: JavaCodeInsightTestFixture,
            wolfErrorsDirective: String
        ): Boolean {
            val text = fixture.getDocument(fixture.file).text
            return InTextDirectivesUtils.findStringWithPrefixes(text, wolfErrorsDirective) == "true"
        }
    }
}