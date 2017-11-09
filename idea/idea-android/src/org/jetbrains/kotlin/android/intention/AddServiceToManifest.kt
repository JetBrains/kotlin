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

package org.jetbrains.kotlin.android.intention

import com.android.SdkConstants
import org.jetbrains.android.dom.manifest.Manifest
import org.jetbrains.kotlin.android.isSubclassOf
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass


class AddServiceToManifest : AbstractRegisterComponentAction("Add service to manifest") {
    override fun isApplicableTo(element: KtClass, manifest: Manifest): Boolean =
            element.isSubclassOfService() && !element.isRegisteredService(manifest)

    override fun applyTo(element: KtClass, manifest: Manifest) = runWriteAction {
        val psiClass = element.toLightClass() ?: return@runWriteAction
        manifest.application.addService().serviceClass.value = psiClass
    }

    private fun KtClass.isRegisteredService(manifest: Manifest) = manifest.application.services.any {
        it.serviceClass.value?.qualifiedName == fqName?.asString()
    }

    private fun KtClass.isSubclassOfService() =
            (descriptor as? ClassDescriptor)?.defaultType?.isSubclassOf(SdkConstants.CLASS_SERVICE, true) ?: false
}