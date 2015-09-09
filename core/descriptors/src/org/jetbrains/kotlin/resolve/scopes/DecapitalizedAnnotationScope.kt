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

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

val DECAPITALIZED_DEPRECATED_ANNOTATIONS = arrayOf(
        "Deprecated", "Extension", "Suppress", "Throws",
        "jvm.Volatile", "jvm.Transient", "jvm.Strictfp", "jvm.Synchronized",
        "jvm.JvmOverloads", "jvm.JvmName", "jvm.JvmStatic", "annotation.Target"
).map { FqName("kotlin.$it") }.toSet()

val DECAPITALIZED_SHORT_NAMES = DECAPITALIZED_DEPRECATED_ANNOTATIONS.map { it.shortName().asString().decapitalize() }.toSet()

public class DecapitalizedAnnotationScope(override val workerScope: JetScope) : AbstractScopeAdapter() {
    override fun getClassifier(name: Name, location: LookupLocation)
            = super.getClassifier(name, location)
              ?: findDecapitalizedAnnotation(name, location)

    private fun findDecapitalizedAnnotation(name: Name, location: LookupLocation): ClassifierDescriptor? {
        val nameAsString = name.asString()

        if (nameAsString.length() == 0 || nameAsString[0].isUpperCase() || nameAsString !in DECAPITALIZED_SHORT_NAMES) return null
        val capitalizedIdentifier = Name.identifier(nameAsString.capitalize())

        val capitalizedClassifier = getClassifier(capitalizedIdentifier, location) ?: return null

        return if (capitalizedClassifier.fqNameSafe in DECAPITALIZED_DEPRECATED_ANNOTATIONS)
                    capitalizedClassifier
               else
                    null
    }

    companion object {
        public fun wrapIfNeeded(scope: JetScope, fqName: FqName): JetScope {
            if (fqName.firstSegmentIs(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME)) {
                return DecapitalizedAnnotationScope(scope)
            }
            return scope
        }
    }
}
