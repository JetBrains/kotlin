/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.Errors.JVM_STATIC_IN_PRIVATE_COMPANION
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class JvmStaticInPrivateCompanionChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {

        descriptor.containingDeclaration.safeAs<ClassDescriptor>()?.takeIf {
            it.isCompanionObject && Visibilities.isPrivate(it.visibility.delegate)
        } ?: return

        val jvmStaticAnnotation = descriptor.annotations.findAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) ?: return

        val reportTarget = jvmStaticAnnotation.source.safeAs<KotlinSourceElement>()?.psi ?: declaration
        context.trace.report(JVM_STATIC_IN_PRIVATE_COMPANION.on(reportTarget))
    }
}