/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.nio.file.Path

/**
 * Receives events from the library, which does not log on its own. An integration turns them into log lines, progress or
 * statistics. Every method does nothing by default.
 *
 * Methods are called from the thread doing the work and must not throw.
 */
public interface ProvisioningListener {
    /** Another thread or process is installing [id], and this caller waits for [lockFile]. */
    public fun waitingForLock(id: String, lockFile: Path) {}

    /** A directory without a marker file was deleted to make room for an installation. */
    public fun leftoverRemoved(installation: Path) {}

    /** [id] has been installed into [installation] by this caller. */
    public fun installed(id: String, installation: Path) {}

    public companion object {
        /** A listener that ignores every event. */
        public val None: ProvisioningListener = object : ProvisioningListener {}
    }
}
