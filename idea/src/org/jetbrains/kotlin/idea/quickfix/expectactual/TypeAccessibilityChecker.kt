/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.expectactual

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.types.KotlinType

interface TypeAccessibilityChecker {
    val project: Project
    val targetModule: Module
    var existingFqNames: Collection<String>

    fun incorrectTypes(declaration: KtNamedDeclaration): Collection<FqName?>
    fun incorrectTypes(descriptor: DeclarationDescriptor): Collection<FqName?>
    fun incorrectTypes(type: KotlinType): Collection<FqName?>

    fun checkAccessibility(declaration: KtNamedDeclaration): Boolean
    fun checkAccessibility(descriptor: DeclarationDescriptor): Boolean
    fun checkAccessibility(type: KotlinType): Boolean

    fun <R> runInContext(fqNames: Collection<String>, block: TypeAccessibilityChecker.() -> R): R

    companion object {
        fun create(
            project: Project,
            targetModule: Module,
            existingFqNames: Collection<String> = emptyList()
        ): TypeAccessibilityChecker = TypeAccessibilityCheckerImpl(project, targetModule, existingFqNames)

        @get:TestOnly
        @set:TestOnly
        var testLog: StringBuilder? = null
    }
}