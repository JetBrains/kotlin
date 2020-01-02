/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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


class AddBroadcastReceiverToManifest : AbstractRegisterComponentAction("Add broadcast receiver to manifest") {
    override fun isApplicableTo(element: KtClass, manifest: Manifest): Boolean =
        element.isSubclassOfBroadcastReceiver() && !element.isRegisteredBroadcastReceiver(manifest)

    override fun applyTo(element: KtClass, manifest: Manifest) = runWriteAction {
        val psiClass = element.toLightClass() ?: return@runWriteAction
        manifest.application.addReceiver().receiverClass.value = psiClass
    }

    private fun KtClass.isRegisteredBroadcastReceiver(manifest: Manifest) = manifest.application.receivers.any {
        it.receiverClass.value?.qualifiedName == fqName?.asString()
    }

    private fun KtClass.isSubclassOfBroadcastReceiver() =
        (descriptor as? ClassDescriptor)?.defaultType?.isSubclassOf(SdkConstants.CLASS_BROADCASTRECEIVER, true) ?: false
}