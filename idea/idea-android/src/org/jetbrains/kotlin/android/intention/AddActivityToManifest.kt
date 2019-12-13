/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.intention

import com.android.SdkConstants
import org.jetbrains.android.dom.manifest.Manifest
import org.jetbrains.kotlin.android.isSubclassOf
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.asJava.toLightClass


class AddActivityToManifest : AbstractRegisterComponentAction("Add activity to manifest") {
    override fun isApplicableTo(element: KtClass, manifest: Manifest): Boolean =
        element.isSubclassOfActivity() && !element.isRegisteredActivity(manifest)

    override fun applyTo(element: KtClass, manifest: Manifest) = runWriteAction {
        val psiClass = element.toLightClass() ?: return@runWriteAction
        manifest.application.addActivity().activityClass.value = psiClass
    }

    private fun KtClass.isRegisteredActivity(manifest: Manifest) = manifest.application.activities.any {
        it.activityClass.value?.qualifiedName == fqName?.asString()
    }

    private fun KtClass.isSubclassOfActivity() =
        (descriptor as? ClassDescriptor)?.defaultType?.isSubclassOf(SdkConstants.CLASS_ACTIVITY, true) ?: false
}