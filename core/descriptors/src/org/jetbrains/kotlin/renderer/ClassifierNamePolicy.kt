/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.renderer

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.util.*

interface ClassifierNamePolicy {
    object SHORT : ClassifierNamePolicy {
        override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
            if (classifier is TypeParameterDescriptor) return renderer.renderName(classifier.name, false)

            val qualifiedNameElements = ArrayList<Name>()

            // for nested classes qualified name should be used
            var current: DeclarationDescriptor? = classifier
            do {
                qualifiedNameElements.add(current!!.name)
                current = current.containingDeclaration
            }
            while (current is ClassDescriptor)

            return renderFqName(qualifiedNameElements.asReversed())
        }
    }

    object FULLY_QUALIFIED : ClassifierNamePolicy {
        override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
            if (classifier is TypeParameterDescriptor) return renderer.renderName(classifier.name, false)

            return renderer.renderFqName(DescriptorUtils.getFqName(classifier))
        }
    }

    // for local declarations qualified up to function scope
    object SOURCE_CODE_QUALIFIED : ClassifierNamePolicy {
        override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String =
                qualifiedNameForSourceCode(classifier)

        private fun qualifiedNameForSourceCode(descriptor: ClassifierDescriptor): String {
            val nameString = descriptor.name.render()
            if (descriptor is TypeParameterDescriptor) {
                return nameString
            }
            val qualifier = qualifierName(descriptor.containingDeclaration)
            return if (qualifier != null && qualifier != "") qualifier + "." + nameString else nameString
        }

        private fun qualifierName(descriptor: DeclarationDescriptor): String? = when (descriptor) {
            is ClassDescriptor -> qualifiedNameForSourceCode(descriptor)
            is PackageFragmentDescriptor -> descriptor.fqName.toUnsafe().render()
            else -> null
        }
    }

    fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String
}