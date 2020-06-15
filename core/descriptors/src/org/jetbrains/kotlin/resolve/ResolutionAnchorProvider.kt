/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

interface ResolutionAnchorProvider {
    fun getResolutionAnchor(moduleDescriptor: ModuleDescriptor): ModuleDescriptor? = null

    companion object {
        val Default: ResolutionAnchorProvider = object : ResolutionAnchorProvider {
            override fun getResolutionAnchor(moduleDescriptor: ModuleDescriptor): ModuleDescriptor? = null
        }
    }
}

val RESOLUTION_ANCHOR_PROVIDER_CAPABILITY = ModuleCapability<ResolutionAnchorProvider>("ResolutionAnchorProvider")

fun ModuleDescriptor.getResolutionAnchorIfAny(): ModuleDescriptor? =
    getCapability(RESOLUTION_ANCHOR_PROVIDER_CAPABILITY)?.getResolutionAnchor(this)
