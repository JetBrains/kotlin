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

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.JUnitRecognizer
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.caches.project.implementingDescriptors
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.platform.impl.isCommon
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered

class KotlinMultiplatformJUnitRecognizer : JUnitRecognizer() {
    override fun isTestAnnotated(method: PsiMethod): Boolean {
        if (method !is KtLightMethod) return false
        val origin = method.kotlinOrigin ?: return false
        if (!origin.module?.platform.isCommon) return false

        val moduleDescriptor = origin.containingKtFile.findModuleDescriptor()
        val implModules = moduleDescriptor.implementingDescriptors
        if (implModules.isEmpty()) return false

        val methodDescriptor = origin.resolveToDescriptorIfAny() ?: return false
        return methodDescriptor.annotations.any { it.isExpectOfAnnotation("org.junit.Test", implModules) }
    }
}

private fun AnnotationDescriptor.isExpectOfAnnotation(fqName: String, implModules: Collection<ModuleDescriptor>): Boolean {
    val annotationClass = annotationClass ?: return false
    if (!annotationClass.isExpect) return false
    val classId = annotationClass.classId ?: return false
    val segments = classId.relativeClassName.pathSegments()

    return implModules
        .any { module ->
            module
                .getPackage(classId.packageFqName).memberScope
                .getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS) { it == segments.first() }
                .filterIsInstance<TypeAliasDescriptor>()
                .any { it.classDescriptor?.fqNameSafe?.asString() == fqName }
        }
}
