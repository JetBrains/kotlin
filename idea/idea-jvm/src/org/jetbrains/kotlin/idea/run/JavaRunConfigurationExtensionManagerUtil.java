/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run;

import com.intellij.execution.JavaRunConfigurationExtensionManager;

// Introduced for compatibility reasons only.
// Method JavaRunConfigurationExtensionManager.getInstance() was rewritten to Kotlin in 191 and
// can't be called from Kotlin anymore.
// BUNCH: 183
public class JavaRunConfigurationExtensionManagerUtil {
    public static JavaRunConfigurationExtensionManager getInstance() {
        //noinspection IncompatibleAPI
        return JavaRunConfigurationExtensionManager.getInstance();
    }
}
