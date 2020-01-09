/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.extensions.Extensions
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
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.createSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaConstructor
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.scopes.JavaClassEnhancementScope
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.buildUseSiteMemberScope
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.KotlinTestUtils.getAnnotationsJar
import org.jetbrains.kotlin.test.KotlinTestUtils.newConfiguration
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File
import java.io.IOException

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
        super.tearDown()
    }

    private fun createJarWithForeignAnnotations(): File =
        MockLibraryUtil.compileJavaFilesLibraryToJar(FOREIGN_ANNOTATIONS_SOURCES_PATH, "foreign-annotations")

    private fun createEnvironment(content: String): KotlinCoreEnvironment {
        val classpath = mutableListOf(getAnnotationsJar(), ForTestCompileRuntime.runtimeJarForTests())
        if (InTextDirectivesUtils.isDirectiveDefined(content, "ANDROID_ANNOTATIONS")) {
            classpath.add(ForTestCompileRuntime.androidAnnotationsForTests())
        }
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
            Extensions.getArea(project)
                .getExtensionPoint(PsiElementFinder.EP_NAME)
                .unregisterExtension(JavaElementFinder::class.java)
        }
    }

    fun doTest(path: String) {
        val javaFile = File(path)
        val javaLines = javaFile.readLines()
        val content = javaLines.joinToString(separator = "\n")
        if (InTextDirectivesUtils.isDirectiveDefined(content, "SKIP_IN_FIR_TEST")) return

        val srcFiles = TestFiles.createTestFiles<Void, File>(
            javaFile.name, FileUtil.loadFile(javaFile, true),
            object : TestFiles.TestFileFactoryNoModules<File>() {
                override fun create(fileName: String, text: String, directives: Map<String, String>): File {
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
            }, ""
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
        val session = createSession(environment, scope)

        val topPsiClasses = psiFiles.flatMap { it.getChildrenOfType<PsiClass>().toList() }

        val javaFirDump = StringBuilder().also { builder ->
            val renderer = FirRenderer(builder)
            val symbolProvider = session.firSymbolProvider as FirCompositeSymbolProvider
            val javaProvider = symbolProvider.providers.filterIsInstance<JavaSymbolProvider>().first()

            fun processClassWithChildren(psiClass: PsiClass, parentFqName: FqName) {
                val psiFile = psiClass.containingFile
                val packageStatement = psiFile.children.filterIsInstance<PsiPackageStatement>().firstOrNull()
                val packageName = packageStatement?.packageName
                val fqName = parentFqName.child(Name.identifier(psiClass.name!!))
                val classId = ClassId(packageName?.let { FqName(it) } ?: FqName.ROOT, fqName, false)
                javaProvider.getClassLikeSymbolByFqName(classId)
                    ?: throw AssertionError(classId.asString())
                psiClass.innerClasses.forEach {
                    processClassWithChildren(psiClass = it, parentFqName = fqName)
                }
            }
            for (psiClass in topPsiClasses) {
                processClassWithChildren(psiClass, FqName.ROOT)
            }

            val processedJavaClasses = mutableSetOf<FirJavaClass>()
            for (javaClass in javaProvider.getJavaTopLevelClasses().sortedBy { it.name }) {
                if (javaClass !is FirJavaClass || javaClass in processedJavaClasses) continue
                val enhancementScope = javaClass.buildUseSiteMemberScope(session, ScopeSession()).let {
                    when (it) {
                        is FirCompositeScope -> it.scopes.filterIsInstance<JavaClassEnhancementScope>().first()
                        is JavaClassEnhancementScope -> it
                        else -> null
                    }
                }
                if (enhancementScope == null) {
                    javaClass.accept(renderer, null)
                } else {
                    renderer.visitMemberDeclaration(javaClass)
                    renderer.renderSupertypes(javaClass)
                    renderer.renderInBraces {
                        val renderedDeclarations = mutableListOf<FirDeclaration>()
                        for (declaration in javaClass.declarations) {
                            if (declaration in renderedDeclarations) continue
                            when (declaration) {
                                is FirJavaConstructor -> enhancementScope.processFunctionsByName(javaClass.name) { symbol ->
                                    val enhanced = symbol.fir
                                    if (enhanced !in renderedDeclarations) {
                                        enhanced.accept(renderer, null)
                                        renderer.newLine()
                                        renderedDeclarations += enhanced
                                    }
                                }
                                is FirJavaMethod -> enhancementScope.processFunctionsByName(declaration.name) { symbol ->
                                    val enhanced = symbol.fir
                                    if (enhanced !in renderedDeclarations) {
                                        enhanced.accept(renderer, null)
                                        renderer.newLine()
                                        renderedDeclarations += enhanced
                                    }
                                }
                                is FirJavaField -> enhancementScope.processPropertiesByName(declaration.name) { symbol ->
                                    val enhanced = symbol.fir
                                    if (enhanced !in renderedDeclarations) {
                                        enhanced.accept(renderer, null)
                                        renderer.newLine()
                                        renderedDeclarations += enhanced
                                    }
                                }
                                else -> {
                                    declaration.accept(renderer, null)
                                    renderer.newLine()
                                    renderedDeclarations += declaration
                                }
                            }
                        }
                    }
                }
                processedJavaClasses += javaClass
            }
        }.toString()

        val expectedFile = File(javaFile.absolutePath.replace(".java", ".fir.txt"))
        KotlinTestUtils.assertEqualsToFile(expectedFile, javaFirDump)
    }

    companion object {
        private const val FOREIGN_ANNOTATIONS_SOURCES_PATH = "third-party/annotations"
    }
}

abstract class AbstractOwnFirTypeEnhancementTest : AbstractFirTypeEnhancementTest()
