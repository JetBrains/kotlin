/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.ecj.tests

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM.AllJavaSourcesInProjectScope
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.toAbstractProjectFileSearchScope
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.sourceFileProvider

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

    override fun transform(
        module: TestModule,
        inputArtifact: ResultingArtifact.Source,
    ): EcjJavaToFirCompilationArtifact? {

        // We expect the input to be a Java source file
        val testFile = module.files.singleOrNull() ?: error("Expected a single file in module ${module.name}")
        if (!testFile.name.endsWith(".java")) error("Expected a Java source file, got ${testFile.name}")

        val javaFile = testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(testFile)
        val javaSource = testFile.originalContent

        // Extract package and class name from the Java source
        val packageName = extractPackageName(javaSource)
        val className = extractClassName(javaSource)

        if (packageName == null || className == null) {
            return EcjJavaToFirCompilationArtifact(
                sourceFile = javaFile,
                javaSource = javaSource,
                firJavaClass = null,
                diagnostics = listOf("Failed to extract package or class name from Java source"),
            )
        }

        // Create a project and VfsBasedProjectEnvironment
        val project = testServices.compilerConfigurationProvider.getProject(module)
        val packagePartProviderFactory = testServices.compilerConfigurationProvider.getPackagePartProviderFactory(module)
        val projectEnvironment = VfsBasedProjectEnvironment(
            project, VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
        ) { packagePartProviderFactory.invoke(it) }

        // Create a test session
        val session = createTestSession()
        projectEnvironment.registerAsJavaElementFinder(session)
        val moduleData = session.moduleData

        // Create a FirRegularClassSymbol for the class
        val classId = ClassId(FqName(packageName), FqName(className), false)
        val classSymbol = FirRegularClassSymbol(classId)

        // Create a search scope for the temporary file
        val searchScope = AllJavaSourcesInProjectScope(project)
        val projectFileSearchScope = searchScope.toAbstractProjectFileSearchScope()

        // Get the FirJavaFacade from the project environment
        val firJavaFacade = projectEnvironment.getFirJavaFacade(session, moduleData, projectFileSearchScope)

        // Find the Java class
        val javaClass = try {
            firJavaFacade.findClass(classId)
        } catch (e: Exception) {
            return EcjJavaToFirCompilationArtifact(
                sourceFile = javaFile,
                javaSource = javaSource,
                firJavaClass = null,
                diagnostics = listOf("Exception during conversion: ${e.message}"),
            )
        }

        if (javaClass == null) {
            return EcjJavaToFirCompilationArtifact(
                sourceFile = javaFile,
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
                sourceFile = javaFile,
                javaSource = javaSource,
                firJavaClass = null,
                diagnostics = listOf("Exception during conversion to FIR: ${e.message}"),
            )
        }

        return EcjJavaToFirCompilationArtifact(
            sourceFile = javaFile,
            javaSource = javaSource,
            firJavaClass = firJavaClass,
            session = session,
        )
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
