/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.android.ddmlib.IDevice

class AndroidDevice(private val wrapped: IDevice) : Device(wrapped.serialNumber) {
    override fun getDisplayName(): String =
        "${wrapped.avdName.replace('_', ' ')} | Android ${wrapped.version.apiString}"
}