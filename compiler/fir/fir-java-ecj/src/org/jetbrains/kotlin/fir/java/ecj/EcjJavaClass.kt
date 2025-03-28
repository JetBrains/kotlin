/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.ecj

import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants
import org.jetbrains.kotlin.name.ClassId

/**
 * Represents a Java class after ECJ processing.
 *
 * This class provides functionality to iterate over Java class declarations (API-related ones)
 * and pass them to a lambda for further processing.
 */
class EcjJavaClass(
    val classId: ClassId,
    private val typeDeclaration: TypeDeclaration
) {
    /**
     * Iterates over Java class declarations (API-related ones) and passes them to the provided [processor].
     *
     * @param processor A lambda that processes each declaration.
     */
    fun <T> processApiDeclarations(processor: (Any) -> T): List<T> {
        val results = mutableListOf<T>()

        // Process the main class declaration
        if (isApiRelated(typeDeclaration)) {
            results.add(processor(typeDeclaration))

            // Process methods
            typeDeclaration.methods?.forEach { method ->
                if (isApiRelated(method)) {
                    results.add(processor(method))
                }
            }

            // Process fields
            typeDeclaration.fields?.forEach { field ->
                if (isApiRelated(field)) {
                    results.add(processor(field))
                }
            }
        }

        // Process member types (nested classes)
        typeDeclaration.memberTypes?.forEach { memberType ->
            if (isApiRelated(memberType)) {
                results.add(processor(memberType))
            }
        }

        return results
    }

    /**
     * Determines if a declaration is API-related (public or otherwise affecting the API).
     */
    private fun isApiRelated(declaration: Any): Boolean {
        // Check if the declaration is public or protected
        val modifiers = when (declaration) {
            is TypeDeclaration -> declaration.modifiers
            is MethodDeclaration -> declaration.modifiers
            is FieldDeclaration -> declaration.modifiers
            else -> return false
        }
        return modifiers and (ClassFileConstants.AccPublic or ClassFileConstants.AccProtected) != 0
    }
}
