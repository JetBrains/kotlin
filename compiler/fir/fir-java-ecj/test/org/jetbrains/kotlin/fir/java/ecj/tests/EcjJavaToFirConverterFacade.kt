/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.ecj.tests

import org.jetbrains.kotlin.fir.java.ecj.EcjJavaClassFinder
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Files

/**
 * Facade for converting Java source files to FIR using ECJ.
 */
class EcjJavaToFirConverterFacade(
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
        val file = module.files.singleOrNull() ?: error("Expected a single file in module ${module.name}")
        if (!file.name.endsWith(".java")) error("Expected a Java source file, got ${file.name}")

        val javaSource = file.originalContent

        // Create a temporary file with the Java source code
        val tempFile = Files.createTempFile("ecj_test", ".java").toFile()
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

            // Create an EcjJavaClassFinder with the temporary file
            val finder = EcjJavaClassFinder(listOf(tempFile))

            // Find the class
            val classId = ClassId(FqName(packageName), FqName(className), false)
            val ecjJavaClass = finder.findClass(classId)
                ?: return EcjJavaToFirCompilationArtifact(
                    sourceFile = file.originalFile,
                    javaSource = javaSource,
                    firJavaClass = null,
                    diagnostics = listOf("Class not found: $classId"),
                )

            // Create a simple FirSession for testing
            val session = createTestSession()
            val moduleData = session.moduleData

            // Create a FirRegularClassSymbol for the class
            val classSymbol = FirRegularClassSymbol(classId)

            // Convert the EcjJavaClass to a FirJavaClass
            val firJavaClass = ecjJavaClass.convertJavaClassToFir(classSymbol, null, moduleData)

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

