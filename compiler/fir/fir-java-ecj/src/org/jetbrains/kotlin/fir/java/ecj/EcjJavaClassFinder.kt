/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.ecj

import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
import org.eclipse.jdt.internal.compiler.parser.Parser
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * A class finder that uses ECJ to parse Java source files.
 *
 * @property sourceFiles The list of Java source files to be processed.
 */
class EcjJavaClassFinder(private val sourceFiles: List<File>) {
    // Lazily initialized map of ClassId to TypeDeclaration
    private val typeDeclarationCache by lazy { buildTypeDeclarationCache() }

    /**
     * Finds a Java class by its [ClassId].
     *
     * This method lazily "compiles" the sources by ECJ to a point where we can create the [EcjJavaClass].
     *
     * @param classId The ClassId of the class to find.
     * @return An [EcjJavaClass] representing the found class, or null if the class was not found.
     */
    fun findClass(classId: ClassId): EcjJavaClass? {
        val typeDeclaration = typeDeclarationCache[classId] ?: return null
        return EcjJavaClass(classId, typeDeclaration)
    }

    /**
     * Builds a cache of ClassId to TypeDeclaration mappings by parsing all source files.
     */
    private fun buildTypeDeclarationCache(): Map<ClassId, TypeDeclaration> {
        val result = mutableMapOf<ClassId, TypeDeclaration>()

        // Create compilation units from source files
        val compilationUnits = sourceFiles.map { file ->
            val content = file.readText(StandardCharsets.UTF_8).toCharArray()
            CompilationUnit(content, file.absolutePath, StandardCharsets.UTF_8.name())
        }.toTypedArray()

        // Set up compiler options
        val options = CompilerOptions().apply {
            sourceLevel = ClassFileConstants.JDK11
            targetJDK = sourceLevel
            // Enable parser
            parseLiteralExpressionsAsConstants = true
            complianceLevel = sourceLevel
        }

        // Create a problem reporter and parser
        val problemReporter = ProblemReporter(
            DefaultErrorHandlingPolicies.proceedWithAllProblems(),
            options,
            DefaultProblemFactory(Locale.getDefault())
        )
        val parser = Parser(problemReporter, true)

        // Parse each source file
        for (unit in compilationUnits) {
            try {
                // Create a compilation result
                val compilationResult = org.eclipse.jdt.internal.compiler.CompilationResult(
                    unit,
                    0,
                    1,
                    options.maxProblemsPerUnit
                )

                // Parse the compilation unit
                val parsedUnit = parser.parse(unit, compilationResult)
                if (parsedUnit != null) {
                    processCompilationUnit(parsedUnit, result)
                }
            } catch (e: Exception) {
                // Log the error for debugging
                println("[DEBUG_LOG] Error parsing ${unit.fileName}: ${e.message}")
                e.printStackTrace()
            }
        }

        return result
    }

    /**
     * Processes a compilation unit to extract type declarations and map them to ClassIds.
     */
    private fun processCompilationUnit(unit: CompilationUnitDeclaration, result: MutableMap<ClassId, TypeDeclaration>) {
        unit.types?.forEach { typeDeclaration ->
            val packageName = unit.currentPackage?.let { pkg ->
                pkg.tokens.joinToString(".") { String(it) }
            } ?: ""
            val className = String(typeDeclaration.name)
            val classId = ClassId(FqName(packageName), FqName(className), false)

            result[classId] = typeDeclaration

            // Process nested types
            processNestedTypes(typeDeclaration, classId, result)
        }
    }

    /**
     * Processes nested types recursively.
     */
    private fun processNestedTypes(typeDeclaration: TypeDeclaration, parentClassId: ClassId, result: MutableMap<ClassId, TypeDeclaration>) {
        typeDeclaration.memberTypes?.forEach { memberType ->
            val nestedClassName = String(memberType.name)
            val nestedClassId = parentClassId.createNestedClassId(Name.identifier(nestedClassName))

            result[nestedClassId] = memberType

            // Process nested types recursively
            processNestedTypes(memberType, nestedClassId, result)
        }
    }
}
