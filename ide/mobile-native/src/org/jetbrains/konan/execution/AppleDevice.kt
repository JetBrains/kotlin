/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.jetbrains.cidr.execution.deviceSupport.AMDevice
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorConfiguration

abstract class AppleDevice(id: String, name: String, osVersion: String) : Device(id, name, "iOS", osVersion)

class ApplePhysicalDevice(private val wrapped: AMDevice) : AppleDevice(
    wrapped.deviceIdentifier,
    wrapped.name,
    wrapped.productVersion ?: "Unknown"
)

class AppleSimulator(private val wrapped: SimulatorConfiguration) : AppleDevice(
    wrapped.udid,
    wrapped.name,
    wrapped.version
)