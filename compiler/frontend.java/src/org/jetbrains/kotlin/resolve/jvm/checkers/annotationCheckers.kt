/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.AdditionalAnnotationChecker
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAnnotationRetention
import org.jetbrains.kotlin.resolve.descriptorUtil.isRepeatableAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

object RepeatableAnnotationChecker: AdditionalAnnotationChecker {
    override fun checkEntries(entries: List<KtAnnotationEntry>, actualTargets: List<KotlinTarget>, trace: BindingTrace) {
        val entryTypesWithAnnotations = hashMapOf<FqName, MutableList<AnnotationUseSiteTarget?>>()

        for (entry in entries) {
            val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: continue
            val fqName = descriptor.fqName ?: continue
            val classDescriptor = descriptor.annotationClass ?: continue

            val useSiteTarget = entry.useSiteTarget?.getAnnotationUseSiteTarget()
            val existingTargetsForAnnotation = entryTypesWithAnnotations.getOrPut(fqName) { arrayListOf() }
            val duplicateAnnotation = useSiteTarget in existingTargetsForAnnotation
                                      || (existingTargetsForAnnotation.any { (it == null) != (useSiteTarget == null) })

            if (duplicateAnnotation
                && classDescriptor.isRepeatableAnnotation()
                && classDescriptor.getAnnotationRetention() != KotlinRetention.SOURCE) {
                trace.report(ErrorsJvm.NON_SOURCE_REPEATED_ANNOTATION.on(entry))
            }

            existingTargetsForAnnotation.add(useSiteTarget)
        }
    }
}

object FileClassAnnotationsChecker: AdditionalAnnotationChecker {
    // JvmName & JvmMultifileClass annotations are applicable to multi-file class parts regardless of their retention.
    private val alwaysApplicable = hashSetOf(JvmFileClassUtil.JVM_NAME, JvmFileClassUtil.JVM_MULTIFILE_CLASS)

    override fun checkEntries(entries: List<KtAnnotationEntry>, actualTargets: List<KotlinTarget>, trace: BindingTrace) {
        val fileAnnotationsToCheck = arrayListOf<Pair<KtAnnotationEntry, ClassDescriptor>>()
        for (entry in entries) {
            if (entry.useSiteTarget?.getAnnotationUseSiteTarget() != AnnotationUseSiteTarget.FILE) continue
            val descriptor = trace.get(BindingContext.ANNOTATION, entry) ?: continue
            val classDescriptor = descriptor.annotationClass ?: continue
            // This check matters for the applicable annotations only.
            val applicableTargets = AnnotationChecker.applicableTargetSet(classDescriptor)
            if (applicableTargets == null || !applicableTargets.contains(KotlinTarget.FILE)) continue
            fileAnnotationsToCheck.add(Pair(entry, classDescriptor))
        }

        val isMultifileClass = fileAnnotationsToCheck.any { it.second.fqNameSafe == JvmFileClassUtil.JVM_MULTIFILE_CLASS }

        if (isMultifileClass) {
            for ((entry, classDescriptor) in fileAnnotationsToCheck) {
                val classFqName = classDescriptor.fqNameSafe
                if (classFqName in alwaysApplicable) continue
                if (classDescriptor.getAnnotationRetention() != KotlinRetention.SOURCE) {
                    trace.report(ErrorsJvm.ANNOTATION_IS_NOT_APPLICABLE_TO_MULTIFILE_CLASSES.on(entry, classFqName))
                }
            }
        } else {
            for ((entry, classDescriptor) in fileAnnotationsToCheck) {
                if (classDescriptor.fqNameSafe != JvmFileClassUtil.JVM_PACKAGE_NAME) continue

                val argumentExpression = entry.valueArguments.firstOrNull()?.getArgumentExpression() ?: continue
                val stringTemplateEntries = (argumentExpression as? KtStringTemplateExpression)?.entries ?: continue
                if (stringTemplateEntries.size > 1) continue

                val value = (stringTemplateEntries.singleOrNull() as? KtLiteralStringTemplateEntry)?.text
                if (value == null) {
                    trace.report(ErrorsJvm.JVM_PACKAGE_NAME_CANNOT_BE_EMPTY.on(entry))
                } else if (!isValidJavaFqName(value)) {
                    trace.report(ErrorsJvm.JVM_PACKAGE_NAME_MUST_BE_VALID_NAME.on(entry))
                } else if (entry.containingKtFile.declarations.any {
                        it !is KtFunction && it !is KtProperty && it !is KtTypeAlias
                    }) {
                    trace.report(ErrorsJvm.JVM_PACKAGE_NAME_NOT_SUPPORTED_IN_FILES_WITH_CLASSES.on(entry))
                }
            }
        }
    }
}
