/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_RECORD_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.resolve.jvm.JAVA_LANG_RECORD_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.isJvmRecord
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class JvmRecordApplicabilityChecker(private val jvmTarget: JvmTarget) : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor || declaration !is KtClassOrObject) return

        for (supertypeEntry in declaration.superTypeListEntries) {
            val supertype = context.trace[BindingContext.TYPE, supertypeEntry.typeReference]
            if (supertype?.constructor?.declarationDescriptor?.fqNameOrNull() == JAVA_LANG_RECORD_FQ_NAME) {
                context.trace.report(ErrorsJvm.ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE.on(supertypeEntry))
                return
            }
        }

        if (!descriptor.isJvmRecord()) return

        val reportOn =
            declaration.annotationEntries.firstOrNull { it.shortName == JVM_RECORD_ANNOTATION_FQ_NAME.shortName() }
                ?: declaration

        if (context.moduleDescriptor.resolveTopLevelClass(JAVA_LANG_RECORD_FQ_NAME, NoLookupLocation.FOR_DEFAULT_IMPORTS) == null) {
            context.trace.report(ErrorsJvm.JVM_RECORD_REQUIRES_JDK15.on(reportOn))
            return
        }

        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.JvmRecordSupport)) {
            context.trace.report(
                Errors.UNSUPPORTED_FEATURE.on(
                    reportOn,
                    LanguageFeature.JvmRecordSupport to context.languageVersionSettings
                )
            )
            return
        }

        if (!jvmTarget.areRecordsAllowed(context.languageVersionSettings.getFlag(JvmAnalysisFlags.enableJvmPreview))) {
            context.trace.report(
                ErrorsJvm.JVM_RECORDS_ILLEGAL_BYTECODE_TARGET.on(reportOn)
            )
            return
        }

        if (descriptor.kind == ClassKind.ENUM_CLASS) {
            val modifierOrName =
                declaration.modifierList?.getModifier(KtTokens.ENUM_KEYWORD)
                    ?: declaration.nameIdentifier
                    ?: declaration

            context.trace.report(ErrorsJvm.ENUM_JVM_RECORD.on(modifierOrName))
            return
        }

        if (!descriptor.isFinalClass) {
            val modifierOrName =
                declaration.modifierList?.findOneOfModifiers(KtTokens.ABSTRACT_KEYWORD, KtTokens.OPEN_KEYWORD, KtTokens.SEALED_KEYWORD)
                    ?: declaration.nameIdentifier
                    ?: declaration

            context.trace.report(ErrorsJvm.NON_FINAL_JVM_RECORD.on(modifierOrName))
            return
        }

        if (descriptor.isInner) {
            val modifierOrName =
                declaration.modifierList?.getModifier(KtTokens.INNER_KEYWORD)
                    ?: declaration.nameIdentifier
                    ?: declaration

            context.trace.report(ErrorsJvm.INNER_JVM_RECORD.on(modifierOrName))
            return
        }

        if (DescriptorUtils.isLocal(descriptor)) {
            context.trace.report(ErrorsJvm.LOCAL_JVM_RECORD.on(reportOn))
            return
        }


        for (member in declaration.declarations) {
            if (member !is KtProperty) continue

            val propertyDescriptor = context.trace[BindingContext.DECLARATION_TO_DESCRIPTOR, member] as? PropertyDescriptor ?: continue
            if (context.trace.bindingContext[BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor] != true && member.delegate == null) continue

            context.trace.report(ErrorsJvm.FIELD_IN_JVM_RECORD.on(member))
            return
        }

        for (superTypeEntry in declaration.superTypeListEntries) {
            if (superTypeEntry !is KtDelegatedSuperTypeEntry) continue

            context.trace.report(ErrorsJvm.DELEGATION_BY_IN_JVM_RECORD.on(superTypeEntry))
            return
        }

        for (supertype in descriptor.typeConstructor.supertypes) {
            val classDescriptor = supertype.constructor.declarationDescriptor as? ClassDescriptor ?: continue
            if (classDescriptor.kind == ClassKind.INTERFACE || classDescriptor.fqNameSafe == JAVA_LANG_RECORD_FQ_NAME) continue

            val reportSupertypeOn = declaration.nameIdentifier ?: declaration
            context.trace.report(ErrorsJvm.JVM_RECORD_EXTENDS_CLASS.on(reportSupertypeOn, supertype))
            return
        }

        if (!descriptor.isData) {
            context.trace.report(ErrorsJvm.NON_DATA_CLASS_JVM_RECORD.on(reportOn))
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
    }
}

private fun KtModifierList.findOneOfModifiers(vararg modifierTokens: KtModifierKeywordToken): PsiElement? =
    modifierTokens.firstNotNullOfOrNull(this::getModifier)

private fun JvmTarget.areRecordsAllowed(enableJvmPreview: Boolean): Boolean {
    if (majorVersion < JvmTarget.JVM_15.majorVersion) return false
    return enableJvmPreview || majorVersion > JvmTarget.JVM_15.majorVersion
}
