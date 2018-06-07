/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.update

import com.intellij.ide.plugins.IdeaPluginDescriptor
import org.jetbrains.kotlin.idea.PluginUpdateStatus

// Do an additional verification with PluginUpdateVerifier. Enabled only in AS 32.
fun verify(updateStatus: PluginUpdateStatus.Update): PluginUpdateStatus {
    val pluginDescriptor: IdeaPluginDescriptor = updateStatus.pluginDescriptor
    val pluginVerifiers = PluginUpdateVerifier.EP_NAME.extensions

    val declineMessage = pluginVerifiers.asSequence()
        .map { verifier ->
            val verifyResult = verifier.verify(pluginDescriptor)
            if (verifyResult != null && !verifyResult.verified) {
                verifyResult.declineMessage
            } else {
                null
            }
        }
        .firstOrNull { reason -> reason != null }

    if (declineMessage != null) {
        return PluginUpdateStatus.Unverified(declineMessage, updateStatus)
    }

    return updateStatus
}