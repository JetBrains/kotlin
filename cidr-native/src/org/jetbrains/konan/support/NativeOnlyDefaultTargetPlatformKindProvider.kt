/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.support

import org.jetbrains.kotlin.platform.DefaultIdeTargetPlatformKindProvider
import org.jetbrains.kotlin.platform.IdePlatform
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformKind

class NativeOnlyDefaultTargetPlatformKindProvider : DefaultIdeTargetPlatformKindProvider {
    override val defaultPlatform: IdePlatform<*, *>
        get() = CommonIdePlatformKind.defaultPlatform
}