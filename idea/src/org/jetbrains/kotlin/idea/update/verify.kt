/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.update

import org.jetbrains.kotlin.idea.PluginUpdateStatus

// Do an additional verification with PluginUpdateVerifier. Enabled only in AS 3.2+
fun verify(updateStatus: PluginUpdateStatus.Update): PluginUpdateStatus {
    return updateStatus
}