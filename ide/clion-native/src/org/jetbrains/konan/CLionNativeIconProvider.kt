package org.jetbrains.konan

import com.jetbrains.konan.KonanIconProvider
import icons.CLionCMakeIcons
import icons.CLionIcons
import javax.swing.Icon

class CLionNativeIconProvider : KonanIconProvider {
    override fun getExecutableIcon(): Icon = CLionCMakeIcons.CMakeTarget_Executable
}