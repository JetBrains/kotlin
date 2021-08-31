/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiPackageStatement
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.createSessionForTests
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.KotlinTestUtils.newConfiguration
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil.getAnnotationsJar
import java.io.File
import java.io.IOException
import kotlin.reflect.jvm.javaField

@OptIn(SymbolInternals::class)
abstract class AbstractFirTypeEnhancementTest : KtUsefulTestCase() {
    private lateinit var javaFilesDir: File

    private lateinit var environment: KotlinCoreEnvironment

    val project: Project
        get() {
            return environment.project
        }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        javaFilesDir = KotlinTestUtils.tmpDirForTest(this)
    }

    override fun tearDown() {
        FileUtil.delete(javaFilesDir)
        this::environment.javaField!![this] = null
        super.tearDown()
    }

    private fun createJarWithForeignAnnotations(): File =
        MockLibraryUtilExt.compileJavaFilesLibraryToJar(FOREIGN_ANNOTATIONS_SOURCES_PATH, "foreign-annotations")

    private fun createEnvironment(content: String): KotlinCoreEnvironment {
        val classpath = mutableListOf(getAnnotationsJar(), ForTestCompileRuntime.runtimeJarForTests())
        if (InTextDirectivesUtils.isDirectiveDefined(content, "JVM_ANNOTATIONS")) {
            classpath.add(ForTestCompileRuntime.jvmAnnotationsForTests())
        }
        if (InTextDirectivesUtils.isDirectiveDefined(content, "FOREIGN_ANNOTATIONS")) {
            classpath.add(createJarWithForeignAnnotations())
        }
        return KotlinCoreEnvironment.createForTests(
            testRootDisposable,
            newConfiguration(
                ConfigurationKind.JDK_NO_RUNTIME, TestJdkKind.FULL_JDK, classpath, listOf(javaFilesDir)
            ),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).apply {
            PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)
        }
    }

    @OptIn(ObsoleteTestInfrastructure::class)
    fun doTest(path: String) {
        val javaFile = File(path)
        val javaLines = javaFile.readLines()
        val content = javaLines.joinToString(separator = "\n")
        if (InTextDirectivesUtils.isDirectiveDefined(content, "SKIP_IN_FIR_TEST")) return

        val srcFiles = TestFiles.createTestFiles(
            javaFile.name, FileUtil.loadFile(javaFile, true),
            object : TestFiles.TestFileFactoryNoModules<File>() {
                override fun create(fileName: String, text: String, directives: Directives): File {
                    var currentDir = javaFilesDir
                    if ("/" !in fileName) {
                        val packageFqName =
                            text.split("\n").firstOrNull {
                                it.startsWith("package")
                            }?.substringAfter("package")?.trim()?.substringBefore(";")?.let { name ->
                                FqName(name)
                            } ?: FqName.ROOT
                        for (segment in packageFqName.pathSegments()) {
                            currentDir = File(currentDir, segment.asString()).apply { mkdir() }
                        }
                    }
                    val targetFile = File(currentDir, fileName)
                    try {
                        FileUtil.writeToFile(targetFile, text)
                    } catch (e: IOException) {
                        throw AssertionError(e)
                    }

                    return targetFile
                }
            }
        )
        environment = createEnvironment(content)
        val virtualFiles = srcFiles.map {
            object : LightVirtualFile(
                it.name, JavaLanguage.INSTANCE, StringUtilRt.convertLineSeparators(it.readText())
            ) {
                override fun getPath(): String {
                    //TODO: patch LightVirtualFile
                    return "/${it.name}"
                }
            }
        }
        val factory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
        val psiFiles = virtualFiles.map { factory.trySetupPsiForFile(it, JavaLanguage.INSTANCE, true, false)!! }

        val scope = GlobalSearchScope.filesScope(project, virtualFiles)
            .uniteWith(TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope(project))
        val session = createSessionForTests(
            environment.toAbstractProjectEnvironment(),
            scope.toAbstractProjectFileSearchScope()
        )

        val topPsiClasses = psiFiles.flatMap { it.getChildrenOfType<PsiClass>().toList() }

        val javaFirDump = StringBuilder().also { builder ->
            val renderer = FirRenderer(builder)
            val symbolProvider = session.symbolProvider as FirCompositeSymbolProvider
            val javaProvider = symbolProvider.providers.filterIsInstance<JavaSymbolProvider>().first()

            val processedJavaClasses = mutableSetOf<FirJavaClass>()
            fun processClassWithChildren(psiClass: PsiClass, parentFqName: FqName) {
                val classId = psiClass.classId(parentFqName)
                val javaClass = javaProvider.getClassLikeSymbolByFqName(classId)?.fir
                    ?: throw AssertionError(classId.asString())
                if (javaClass !is FirJavaClass || javaClass in processedJavaClasses) {
                    return
                }
                processedJavaClasses += javaClass
                renderJavaClass(renderer, javaClass, session) {
                    for (innerClass in psiClass.innerClasses.sortedBy { it.name }) {
                        processClassWithChildren(innerClass, classId.relativeClassName)
                    }
                }

            }
            for (psiClass in topPsiClasses.sortedBy { it.name }) {
                processClassWithChildren(psiClass, FqName.ROOT)
            }
        }.toString()

        val expectedFile = File(javaFile.absolutePath.replace(".java", ".fir.txt"))
        KotlinTestUtils.assertEqualsToFile(expectedFile, javaFirDump)
    }

    private fun PsiClass.classId(parentFqName: FqName): ClassId {
        val psiFile = this.containingFile
        val packageStatement = psiFile.children.filterIsInstance<PsiPackageStatement>().firstOrNull()
        val packageName = packageStatement?.packageName
        val fqName = parentFqName.child(Name.identifier(this.name!!))
        return ClassId(packageName?.let { FqName(it) } ?: FqName.ROOT, fqName, false)
    }

    companion object {
        private const val FOREIGN_ANNOTATIONS_SOURCES_PATH = "third-party/annotations"
    }
}

abstract class AbstractOwnFirTypeEnhancementTest : AbstractFirTypeEnhancementTest()
