/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.ecj.tests

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.toAbstractProjectFileSearchScope
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import java.nio.file.Files

/**
 * Facade for converting Java source files to FIR using IntelliJ Core.
 */
class IjCoreJavaToFirConverterFacade(
    val testServices: TestServices,
) : AbstractTestFacade<ResultingArtifact.Source, EcjJavaToFirCompilationArtifact>() {

    override val additionalServices: List<ServiceRegistrationData>
        get() = emptyList()

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override val inputKind: TestArtifactKind<ResultingArtifact.Source>
        get() = SourcesKind

    override val outputKind: TestArtifactKind<EcjJavaToFirCompilationArtifact>
        get() = EcjJavaToFirCompilationArtifact.Kind

    override fun shouldTransform(module: TestModule): Boolean = true

    /**
     * Creates a test project for use in the converter.
     */
    private fun createTestProject(disposable: Disposable): Project {
        val configuration = CompilerConfiguration()
        return KotlinCoreEnvironment.createForTests(
            disposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).project
    }

    override fun transform(
        module: TestModule,
        inputArtifact: ResultingArtifact.Source,
    ): EcjJavaToFirCompilationArtifact? {
        // We expect the input to be a Java source file
        val file = module.files.singleOrNull() ?: error("Expected a single file in module ${module.name}")
        if (!file.name.endsWith(".java")) error("Expected a Java source file, got ${file.name}")

        val javaSource = file.originalContent

        // Create a temporary file with the Java source code
        val tempFile = Files.createTempFile("ijcore_test", ".java").toFile()
        try {
            tempFile.writeText(javaSource)

            // Extract package and class name from the Java source
            val packageName = extractPackageName(javaSource)
            val className = extractClassName(javaSource)

            if (packageName == null || className == null) {
                return EcjJavaToFirCompilationArtifact(
                    sourceFile = file.originalFile,
                    javaSource = javaSource,
                    firJavaClass = null,
                    diagnostics = listOf("Failed to extract package or class name from Java source"),
                )
            }

            // Create a test session
            val session = createTestSession()
            val moduleData = session.moduleData

            // Create a FirRegularClassSymbol for the class
            val classId = ClassId(FqName(packageName), FqName(className), false)
            val classSymbol = FirRegularClassSymbol(classId)

            // Create a project and VfsBasedProjectEnvironment
            val disposable = Disposer.newDisposable("IjCoreJavaToFirConverterFacade")
            val project = createTestProject(disposable)
            val projectEnvironment = VfsBasedProjectEnvironment(
                project,
                VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
            ) { PackagePartProvider.Empty }

            // Create a search scope for the temporary file
            val searchScope = ProjectScope.getLibrariesScope(project)
            val projectFileSearchScope = searchScope.toAbstractProjectFileSearchScope()

            // Get the FirJavaFacade from the project environment
            val firJavaFacade = projectEnvironment.getFirJavaFacade(session, moduleData, projectFileSearchScope)

            // Find the Java class
            val javaClass = try {
                firJavaFacade.findClass(classId)
            } catch (e: Exception) {
                return EcjJavaToFirCompilationArtifact(
                    sourceFile = file.originalFile,
                    javaSource = javaSource,
                    firJavaClass = null,
                    diagnostics = listOf("Exception during conversion: ${e.message}"),
                )
            }

            if (javaClass == null) {
                return EcjJavaToFirCompilationArtifact(
                    sourceFile = file.originalFile,
                    javaSource = javaSource,
                    firJavaClass = null,
                    diagnostics = listOf("Class not found: $classId"),
                )
            }

            // Convert the Java class to FIR
            val firJavaClass = try {
                firJavaFacade.convertJavaClassToFir(classSymbol, null, javaClass)
            } catch (e: Exception) {
                return EcjJavaToFirCompilationArtifact(
                    sourceFile = file.originalFile,
                    javaSource = javaSource,
                    firJavaClass = null,
                    diagnostics = listOf("Exception during conversion to FIR: ${e.message}"),
                )
            }

            return EcjJavaToFirCompilationArtifact(
                sourceFile = file.originalFile,
                javaSource = javaSource,
                firJavaClass = firJavaClass,
                session = session,
            )
        } catch (e: Exception) {
            return EcjJavaToFirCompilationArtifact(
                sourceFile = file.originalFile,
                javaSource = javaSource,
                firJavaClass = null,
                diagnostics = listOf("Exception during conversion: ${e.message}"),
            )
        } finally {
            // Clean up the temporary file
            tempFile.delete()
        }
    }

    /**
     * Extracts the package name from the Java source code.
     */
    private fun extractPackageName(javaSource: String): String? {
        val packageRegex = "package\\s+([\\w.]+)\\s*;".toRegex()
        val matchResult = packageRegex.find(javaSource)
        return matchResult?.groupValues?.get(1)
    }

    /**
     * Extracts the class name from the Java source code.
     */
    private fun extractClassName(javaSource: String): String? {
        val classRegex = "(?:public\\s+)?(?:class|interface|enum)\\s+(\\w+)".toRegex()
        val matchResult = classRegex.find(javaSource)
        return matchResult?.groupValues?.get(1)
    }
}
