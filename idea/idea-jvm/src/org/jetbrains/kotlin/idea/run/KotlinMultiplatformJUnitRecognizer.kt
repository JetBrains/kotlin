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
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.highlighter.allImplementingCompatibleModules
import org.jetbrains.kotlin.idea.highlighter.markers.actualsFor
import org.jetbrains.kotlin.idea.project.targetPlatform
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KotlinMultiplatformJUnitRecognizer : JUnitRecognizer() {
    override fun isTestAnnotated(method: PsiMethod): Boolean {
        if (method !is KtLightMethod) return false
        val origin = method.kotlinOrigin ?: return false
        if (origin.module?.targetPlatform !is TargetPlatformKind.Common) return false

        val moduleDescriptor = origin.containingKtFile.findModuleDescriptor()
        val implModules = moduleDescriptor.allImplementingCompatibleModules
        if (implModules.isEmpty()) return false

        val bindingContext = origin.analyze(BodyResolveMode.PARTIAL)
        val methodDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, origin] ?: return false
        return methodDescriptor.annotations.getAllAnnotations().any { it.isExpectOfAnnotation("org.junit.Test", implModules) }

    }
}

private fun AnnotationWithTarget.isExpectOfAnnotation(fqName: String, implModules: Collection<ModuleDescriptor>): Boolean {
    val annotationClass = annotation.type.constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters ?: return false
    if (!annotationClass.isExpect) return false

    return implModules
        .any { module ->
            module.actualsFor(annotationClass, checkCompatible = false)
                .filterIsInstance<TypeAliasDescriptor>()
                .any { it.classDescriptor?.fqNameSafe?.asString() == fqName }
        }
}
