/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.propertyVisitor

class LateinitVarOverridesLateinitVarInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = propertyVisitor(fun(property) {
        if (!property.hasModifier(KtTokens.OVERRIDE_KEYWORD) || !property.hasModifier(KtTokens.LATEINIT_KEYWORD) || !property.isVar) return
        val identifier = property.nameIdentifier ?: return
        val descriptor = property.resolveToDescriptorIfAny() ?: return
        if (descriptor.overriddenDescriptors.none { (it as? PropertyDescriptor)?.let { d -> d.isLateInit && d.isVar } == true }) return
        holder.registerProblem(
            identifier,
            "lateinit var overrides lateinit var"
        )
    })
}
