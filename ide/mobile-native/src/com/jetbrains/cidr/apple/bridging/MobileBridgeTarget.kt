package com.jetbrains.cidr.apple.bridging

import com.jetbrains.cidr.apple.gradle.AppleTargetModel
import com.jetbrains.swift.symbols.SwiftBridgeTarget

data class MobileBridgeTarget(val targetModel: AppleTargetModel) : SwiftBridgeTarget