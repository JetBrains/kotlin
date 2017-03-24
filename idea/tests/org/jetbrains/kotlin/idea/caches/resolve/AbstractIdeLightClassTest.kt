/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.*
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.asJava.builder.LightClassConstructionContext
import org.jetbrains.kotlin.asJava.builder.StubComputationTracker
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.KotlinDaemonAnalyzerTestCase
import org.jetbrains.kotlin.idea.caches.resolve.LightClassLazinessChecker.Tracker.Level.*
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.IDELightClassConstructionContext
import org.jetbrains.kotlin.idea.completion.test.withServiceRegistered
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.utils.keysToMap
import org.junit.Assert
import java.io.File
import kotlin.test.assertEquals

abstract class AbstractIdeLightClassTest : KotlinLightCodeInsightFixtureTestCase() {
    fun doTest(testDataPath: String) {
        val extraFilePath = testDataPath.replace(".kt", ".extra.kt")
        val testFiles = if (File(extraFilePath).isFile) listOf(testDataPath, extraFilePath) else listOf(testDataPath)


        val lazinessMode = lazinessModeByFileText(testDataPath)
        myFixture.configureByFiles(*testFiles.toTypedArray())

        val ktFile = myFixture.file as KtFile
        testLightClass(testDataPath, { LightClassTestCommon.removeEmptyDefaultImpls(it) }) { fqName ->
            val tracker = LightClassLazinessChecker.Tracker(fqName)
            project.withServiceRegistered<StubComputationTracker, PsiClass?>(tracker) {
                findClass(fqName, ktFile, project)?.apply {
                    LightClassLazinessChecker.check(this as KtLightClass, tracker, lazinessMode)
                    tracker.allowLevel(EXACT)
                    PsiElementChecker.checkPsiElementStructure(this)
                }
            }
        }
    }

    private fun lazinessModeByFileText(testDataPath: String): LightClassLazinessChecker.Mode {
        return File(testDataPath).readText().run {
            val argument = substringAfter("LAZINESS:", "").substringBefore(" ")
            LightClassLazinessChecker.Mode.values().firstOrNull { it.name == argument } ?: LightClassLazinessChecker.Mode.AllChecks
        }
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}

abstract class AbstractIdeCompiledLightClassTest : KotlinDaemonAnalyzerTestCase() {
    override fun setUp() {
        super.setUp()

        val testName = getTestName(false)
        if (KotlinTestUtils.isAllFilesPresentTest(testName)) return

        val filePath = "${KotlinTestUtils.getTestsRoot(this::class.java)}/${getTestName(false)}.kt"

        Assert.assertTrue("File doesn't exist $filePath", File(filePath).exists())

        val libraryJar = MockLibraryUtil.compileLibraryToJar(filePath, libName(), false, false, false)
        val jarUrl = "jar://" + FileUtilRt.toSystemIndependentName(libraryJar.absolutePath) + "!/"
        ModuleRootModificationUtil.addModuleLibrary(module, jarUrl)
    }

    private fun libName() = "libFor" + getTestName(false)

    fun doTest(testDataPath: String) {
        testLightClass(testDataPath, { it }) {
            findClass(it, null, project)?.apply {
                PsiElementChecker.checkPsiElementStructure(this)
            }
        }
    }
}

private fun testLightClass(testDataPath: String, normalize: (String) -> String, findLightClass: (String) -> PsiClass?) {
    LightClassTestCommon.testLightClass(
            File(testDataPath),
            findLightClass,
            normalizeText = { text ->
                //NOTE: ide and compiler differ in names generated for parameters with unspecified names
                text
                        .replace("java.lang.String s,", "java.lang.String p,")
                        .replace("java.lang.String s)", "java.lang.String p)")
                        .replace("java.lang.String s1", "java.lang.String p1")
                        .replace("java.lang.String s2", "java.lang.String p2")
                        .removeLinesStartingWith("@" + JvmAnnotationNames.METADATA_FQ_NAME.asString())
                        .run(normalize)
            }
    )
}

private fun findClass(fqName: String, ktFile: KtFile?, project: Project): PsiClass? {
    return JavaPsiFacade.getInstance(project).findClass(fqName, GlobalSearchScope.allScope(project)) ?:
           PsiTreeUtil.findChildrenOfType(ktFile, KtClassOrObject::class.java)
                   .find { fqName.endsWith(it.nameAsName!!.asString()) }
                   ?.let { KtLightClassForSourceDeclaration.create(it) }
}

object LightClassLazinessChecker {

    enum class Mode {
        AllChecks,
        NoLaziness,
        NoConsistency
    }

    class Tracker(private val fqName: String) : StubComputationTracker {

        private var level = NONE
            set(newLevel) {
                if (newLevel.ordinal <= field.ordinal) {
                    error("Level should not decrease at any point: $level -> $newLevel, allowed: $allowedLevel")
                }
                if (newLevel.ordinal > allowedLevel.ordinal) {
                    error("Level increased before it was expected: $level -> $newLevel, allowed: $allowedLevel")
                }
                field = newLevel
            }

        private var allowedLevel = NONE

        enum class Level {
            NONE,
            LIGHT,
            EXACT
        }

        override fun onStubComputed(javaFileStub: PsiJavaFileStub, context: LightClassConstructionContext) {
            if (fqName != javaFileStub.classes.single().qualifiedName!!) return
            if (context !is IDELightClassConstructionContext) error("Unknown context ${context::class}")
            level = when (context.mode) {
                IDELightClassConstructionContext.Mode.LIGHT -> LIGHT
                IDELightClassConstructionContext.Mode.EXACT -> EXACT
            }
        }

        fun checkLevel(expectedLevel: Level) {
            assert(level == expectedLevel)
        }

        fun allowLevel(newAllowed: Level) {
            allowedLevel = newAllowed
        }
    }

    fun check(lightClass: KtLightClass, tracker: Tracker, lazinessMode: Mode) {
        // lighter classes not implemented for locals
        if (lightClass.kotlinOrigin?.isLocal ?: false) return

        tracker.allowLevel(LIGHT)

        if (lazinessMode != Mode.AllChecks) {
            tracker.allowLevel(EXACT)
        }

        // collect method class results on light members that should not trigger exact context evaluation
        val fieldsToInfo = lightClass.fields.asList().keysToMap { fieldInfo(it) }
        val methodsToInfo = lightClass.methods.asList().keysToMap { methodInfo(it) }

        tracker.allowLevel(EXACT)

        lightClass.clsDelegate // trigger exact context

        tracker.checkLevel(EXACT)

        // still running code above to catch possible exceptions
        if (lazinessMode == Mode.NoConsistency) return

        // check collected data against delegates which should contain correct data
        for ((field, lightFieldInfo) in fieldsToInfo) {
            val delegate = (field as KtLightField).clsDelegate
            assertEquals(fieldInfo(delegate), lightFieldInfo)
        }
        for ((method, lightMethodInfo) in methodsToInfo) {
            val delegate = (method as KtLightMethod).clsDelegate
            assertEquals(methodInfo(delegate), lightMethodInfo)
        }
    }

    private data class FieldInfo(
            val name: String,
            val modifiers: List<String>
    )

    private fun fieldInfo(field: PsiField) = with(field) {
        FieldInfo(
                name!!, PsiModifier.MODIFIERS.asList().filter { modifierList!!.hasModifierProperty(it) }
        )
    }

    private data class MethodInfo(
            val name: String,
            val modifiers: List<String>,
            val isConstructor: Boolean,
            val parameterCount: Int,
            val isVarargs: Boolean
    )

    private fun methodInfo(method: PsiMethod) = with(method) {
        MethodInfo(
                name, PsiModifier.MODIFIERS.asList().filter { modifierList.hasModifierProperty(it) },
                isConstructor, method.parameterList.parametersCount, isVarArgs
        )
    }
}

private fun String.removeLinesStartingWith(prefix: String): String {
    return lines().filterNot { it.trimStart().startsWith(prefix) }.joinToString(separator = "\n")
}
