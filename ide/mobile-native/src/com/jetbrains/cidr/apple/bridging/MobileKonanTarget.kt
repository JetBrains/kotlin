package com.jetbrains.cidr.apple.bridging

import org.jetbrains.konan.resolve.konan.KonanTarget

data class MobileKonanTarget(override val moduleId: String, override val productModuleName: String) : KonanTarget