/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.konan

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KonanTarget

/**
 * A machine that the Kotlin/Native compiler can run on, named by its own target, and the targets it can link binaries for there.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain instances of this interface from [NativePlatformToolchain.supportedHosts].
 *
 * @since 2.5.20
 */
@ExperimentalBuildToolsApi
public interface NativeHost {
    /**
     * The Kotlin/Native target of this host, e.g. [KonanTarget.MACOS_ARM64].
     */
    public val target: KonanTarget

    /**
     * The Kotlin/Native targets this host can link binaries for, e.g. [KonanTarget.IOS_ARM64]. They include [target].
     *
     * A target that a later compiler version adds is left out if this version of the API has no entry for it.
     */
    public val linkableTargets: Set<KonanTarget>
}
