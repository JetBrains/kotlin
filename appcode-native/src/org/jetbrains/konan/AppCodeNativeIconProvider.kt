/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan

import icons.AppcodeIcons
import javax.swing.Icon

class AppCodeNativeIconProvider : CidrNativeIconProvider {
    override fun getExecutableIcon(): Icon = AppcodeIcons.Application //TODO replace with Executable
}