/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.jetbrains.cidr.execution.deviceSupport.AMDevice
import com.jetbrains.cidr.execution.simulatorSupport.SimulatorConfiguration

abstract class AppleDevice(id: String) : Device(id)

class ApplePhysicalDevice(private val wrapped: AMDevice) : AppleDevice(wrapped.deviceIdentifier) {
    override fun getDisplayName(): String = wrapped.name
}

class AppleSimulator(private val wrapped: SimulatorConfiguration) : AppleDevice(wrapped.udid) {
    override fun getDisplayName(): String = "${wrapped.name} | iOS ${wrapped.version}"
}