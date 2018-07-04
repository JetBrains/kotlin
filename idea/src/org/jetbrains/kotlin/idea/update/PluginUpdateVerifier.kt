/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.update

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.extensions.ExtensionPointName

abstract class PluginUpdateVerifier {
    abstract val verifierName: String

    /**
     * @param pluginDescriptor
     * @return null means verifier is not responsible for the given plugin descriptor.
     */
    abstract fun verify(pluginDescriptor: IdeaPluginDescriptor): PluginVerifyResult?

    companion object {
        internal var EP_NAME = ExtensionPointName<PluginUpdateVerifier>("org.jetbrains.kotlin.pluginUpdateVerifier")
    }
}