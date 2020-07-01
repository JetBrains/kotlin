package org.jetbrains.konan

import com.jetbrains.konan.KonanIconProvider
import icons.AppcodeIcons
import javax.swing.Icon

class AppCodeNativeIconProvider : KonanIconProvider {
    override fun getExecutableIcon(): Icon = AppcodeIcons.Application //TODO replace with Executable
}