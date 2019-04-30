/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan

import icons.CLionIcons
import javax.swing.Icon

class CLionNativeIconProvider : CidrNativeIconProvider {
    override fun getExecutableIcon(): Icon = CLionIcons.CMakeTarget_Executable
}