/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor

class LateinitVarOverridesLateinitVarInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = classOrObjectRecursiveVisitor(fun(klass) {
        for (declaration in klass.declarations) {
            val property = declaration as? KtProperty ?: continue
            if (!property.hasModifier(KtTokens.OVERRIDE_KEYWORD) || !property.hasModifier(KtTokens.LATEINIT_KEYWORD) || !property.isVar) {
                continue
            }
            val identifier = property.nameIdentifier ?: continue
            val descriptor = property.resolveToDescriptorIfAny() ?: continue
            if (descriptor.overriddenDescriptors.any { (it as? PropertyDescriptor)?.let { d -> d.isLateInit && d.isVar } == true }) {
                holder.registerProblem(
                    identifier,
                    "lateinit var overrides lateinit var"
                )
            }
        }
    })
}
