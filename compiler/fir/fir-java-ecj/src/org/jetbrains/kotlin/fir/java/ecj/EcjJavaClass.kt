/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.ecj

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
    fun <T> processApiDeclarations(processor: (TypeDeclaration) -> T): List<T> {
        val results = mutableListOf<T>()

        // Process the main class declaration
        if (isApiRelated(typeDeclaration)) {
            results.add(processor(typeDeclaration))
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
     * Determines if a type declaration is API-related (public or otherwise affecting the API).
     */
    private fun isApiRelated(declaration: TypeDeclaration): Boolean {
        // Check if the declaration is public or protected
        return declaration.modifiers and (ClassFileConstants.AccPublic or ClassFileConstants.AccProtected) != 0
    }
}
