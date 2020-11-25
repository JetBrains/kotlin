/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.JAVA_LANG_RECORD_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_RECORD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.isJvmRecord
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

object JvmRecordApplicabilityChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor || declaration !is KtClassOrObject || !descriptor.isJvmRecord()) return

        val reportOn =
            declaration.annotationEntries.firstOrNull { it.shortName == JVM_RECORD_ANNOTATION_FQ_NAME.shortName() }
                ?: declaration

        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.JvmRecordSupport)) {
            context.trace.report(
                Errors.UNSUPPORTED_FEATURE.on(
                    reportOn,
                    LanguageFeature.JvmRecordSupport to context.languageVersionSettings
                )
            )
            return
        }

        if (DescriptorUtils.isLocal(descriptor)) {
            context.trace.report(ErrorsJvm.LOCAL_JVM_RECORD.on(reportOn))
            return
        }

        val primaryConstructor = declaration.primaryConstructor
        val parameters = primaryConstructor?.valueParameters ?: emptyList()
        if (parameters.isEmpty()) {
            (primaryConstructor?.valueParameterList ?: declaration.nameIdentifier)?.let {
                context.trace.report(ErrorsJvm.JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS.on(it))
                return
            }
        }

        for (parameter in parameters) {
            if (!parameter.hasValOrVar() || parameter.isMutable) {
                context.trace.report(ErrorsJvm.JVM_RECORD_NOT_VAL_PARAMETER.on(parameter))
                return
            }
        }

        for (parameter in parameters.dropLast(1)) {
            if (parameter.isVarArg) {
                context.trace.report(ErrorsJvm.JVM_RECORD_NOT_LAST_VARARG_PARAMETER.on(parameter))
                return
            }
        }

        for (supertype in descriptor.typeConstructor.supertypes) {
            val classDescriptor = supertype.constructor.declarationDescriptor as? ClassDescriptor ?: continue
            if (classDescriptor.kind == ClassKind.INTERFACE || classDescriptor.fqNameSafe == JAVA_LANG_RECORD_FQ_NAME) continue

            val reportSupertypeOn = declaration.nameIdentifier ?: declaration
            context.trace.report(ErrorsJvm.JVM_RECORD_EXTENDS_CLASS.on(reportSupertypeOn, supertype))
            return
        }
    }
}
