package com.jetbrains.cidr.apple.bridging

import org.jetbrains.konan.resolve.konan.KonanTarget

data class MobileKonanTarget(val targetName: String) : KonanTarget {
    override val name: String
        get() = targetName

    override val productModuleName: String
        get() = targetName
}