/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.impl.VirtualFileBoundJavaClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryPackageSourceElement
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.ClassifierUsageChecker
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor

class JvmModuleAccessibilityChecker(project: Project) : CallChecker {
    private val moduleResolver = JavaModuleResolver.getInstance(project)

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.resultingDescriptor

        // javac seems to check only the containing class of the member being called. Note that it's fine to call, for example,
        // members with parameter types or return type from an unexported package
        val targetDescriptor = DescriptorUtils.getParentOfType(descriptor, ClassOrPackageFragmentDescriptor::class.java) ?: return

        val fileFromOurModule = DescriptorToSourceUtils.getContainingFile(context.scope.ownerDescriptor)?.virtualFile
        diagnosticFor(targetDescriptor, descriptor, fileFromOurModule, reportOn)?.let(context.trace::report)
    }

    private fun diagnosticFor(
            targetClassOrPackage: ClassOrPackageFragmentDescriptor,
            originalDescriptor: DeclarationDescriptorWithSource?,
            fileFromOurModule: VirtualFile?,
            reportOn: PsiElement
    ): Diagnostic? {
        val referencedFile = findVirtualFile(targetClassOrPackage, originalDescriptor) ?: return null

        // TODO: do not resolve our JavaModule every time, invent a way to obtain it from ModuleDescriptor and do it once in constructor
        val ourModule = fileFromOurModule?.let(moduleResolver::findJavaModule)
        val theirModule = moduleResolver.findJavaModule(referencedFile)

        // If we're both in the unnamed module, it's OK, no error should be reported
        if (ourModule == null && theirModule == null) return null

        if (theirModule == null) {
            // We should probably prohibit this usage according to JPMS (named module cannot use types from unnamed module),
            // but we cannot be sure that a module without module-info.java is going to be actually used as an unnamed module.
            // It could also be an automatic module, in which case it would be read by every module.
            return null
        }

        if (ourModule?.name == theirModule.name) return null

        if (ourModule != null && !moduleResolver.moduleGraph.reads(ourModule.name, theirModule.name)) {
            return ErrorsJvm.JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE.on(reportOn, theirModule.name)
        }

        val containingPackage = DescriptorUtils.getParentOfType(targetClassOrPackage, PackageFragmentDescriptor::class.java, false)
        val fqName = containingPackage?.fqName ?: return null
        if (!theirModule.exports(fqName) && (ourModule == null || !theirModule.exportsTo(fqName, ourModule.name))) {
            return ErrorsJvm.JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE.on(reportOn, theirModule.name, fqName.asString())
        }

        return null
    }

    private fun findVirtualFile(
            descriptor: ClassOrPackageFragmentDescriptor,
            originalDescriptor: DeclarationDescriptorWithSource?
    ): VirtualFile? {
        val source = descriptor.source
        when (source) {
            is KotlinJvmBinarySourceElement -> (source.binaryClass as? VirtualFileKotlinClass)?.file?.let { return it }
            is JavaSourceElement -> (source.javaElement as? VirtualFileBoundJavaClass)?.virtualFile?.let { return it }
            is KotlinJvmBinaryPackageSourceElement -> {
                if (originalDescriptor is DeserializedMemberDescriptor) {
                    (source.getContainingBinaryClass(originalDescriptor) as? VirtualFileKotlinClass)?.file?.let { return it }
                }
            }
        }

        source.getPsi()?.containingFile?.virtualFile?.let { return it }

        return originalDescriptor?.source?.getPsi()?.containingFile?.virtualFile
    }

    inner class ClassifierUsage : ClassifierUsageChecker {
        override fun check(
                targetDescriptor: ClassifierDescriptor,
                trace: BindingTrace,
                element: PsiElement,
                languageVersionSettings: LanguageVersionSettings
        ) {
            val virtualFile = element.containingFile.virtualFile
            when (targetDescriptor) {
                is ClassDescriptor -> {
                    diagnosticFor(targetDescriptor, targetDescriptor, virtualFile, element)?.let(trace::report)
                }
                is TypeAliasDescriptor -> {
                    val containingClassOrPackage = DescriptorUtils.getParentOfType(targetDescriptor, ClassOrPackageFragmentDescriptor::class.java)
                    if (containingClassOrPackage != null) {
                        diagnosticFor(containingClassOrPackage, targetDescriptor, virtualFile, element)?.let(trace::report)
                    }

                    val expandedClass = targetDescriptor.expandedType.constructor.declarationDescriptor as? ClassDescriptor
                    if (expandedClass != null) {
                        diagnosticFor(expandedClass, expandedClass, virtualFile, element)?.let(trace::report)
                    }
                }
            }
        }
    }
}
