/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.getAnnotationRetention
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class JvmAnnotationsTargetNonExistentAccessorChecker : DeclarationChecker {
    companion object {
        private val getterUselessTargets = setOf(PROPERTY_GETTER)
        private val setterUselessTargets = setOf(PROPERTY_SETTER, SETTER_PARAMETER)
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is MemberDescriptor) return
        if (declaration !is KtParameter && declaration !is KtProperty) return

        if (!Visibilities.isPrivate(descriptor.visibility) && !isSpecialStaticProperty(descriptor)) return

        val hasGetterWithBody = declaration is KtProperty && declaration.getter?.hasBody() == true
        val hasSetterWithBody = declaration is KtProperty && declaration.setter?.hasBody() == true

        if (hasGetterWithBody && hasSetterWithBody) return
        if (declaration is KtProperty && declaration.hasDelegate()) return

        val declarationName = declaration.name ?: descriptor.name.asString()

        for (annotation in declaration.annotationEntries) {
            val psiTarget = annotation.useSiteTarget ?: continue
            val useSiteTarget = psiTarget.getAnnotationUseSiteTarget()
            if (!hasGetterWithBody && useSiteTarget in getterUselessTargets ||
                !hasSetterWithBody && useSiteTarget in setterUselessTargets
            ) {
                reportOnAnnotationWithNonSourceRetention(annotation, declarationName, context)
            }
        }

        if (declaration is KtProperty) {
            if (!hasGetterWithBody) {
                declaration.getter?.annotationEntries?.forEach {
                    reportOnAnnotationWithNonSourceRetention(it, declarationName, context)
                }
            }

            if (!hasSetterWithBody) {
                declaration.setter?.annotationEntries?.forEach {
                    reportOnAnnotationWithNonSourceRetention(it, declarationName, context)
                }
            }
        }
    }

    private fun reportOnAnnotationWithNonSourceRetention(
        entry: KtAnnotationEntry,
        declarationName: String,
        context: DeclarationCheckerContext
    ) {
        val annotationDescriptor = context.trace[BindingContext.ANNOTATION, entry] ?: return
        if (annotationDescriptor.annotationClass?.getAnnotationRetention() == KotlinRetention.SOURCE) return

        context.trace.reportDiagnosticOnce(ErrorsJvm.ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR.on(entry, declarationName))
    }

    private fun isSpecialStaticProperty(descriptor: MemberDescriptor): Boolean {
        return descriptor.hasJvmFieldAnnotation() || (descriptor is VariableDescriptor && descriptor.isConst)
    }
}
