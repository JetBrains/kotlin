package org.jetbrains.konan.support

import org.jetbrains.kotlin.platform.DefaultIdeTargetPlatformKindProvider
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms

class NativeOnlyDefaultTargetPlatformKindProvider : DefaultIdeTargetPlatformKindProvider {
    override val defaultPlatform: TargetPlatform
        get() = NativePlatforms.unspecifiedNativePlatform
}