/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("CommonIdePlatformUtil")
package org.jetbrains.kotlin.platform.impl

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.TargetPlatform

object CommonIdePlatformKind : IdePlatformKind<CommonIdePlatformKind>() {

    override fun platformByCompilerArguments(arguments: CommonCompilerArguments): TargetPlatform? {
        return if (arguments is K2MetadataCompilerArguments)
            CommonPlatforms.defaultCommonPlatform
        else
            null
    }

    override fun createArguments(): CommonCompilerArguments {
        return K2MetadataCompilerArguments() // TODO(dsavvinov): review that, as now MPP !== K2Metadata
    }

    override val platforms get() = listOf(CommonPlatforms.defaultCommonPlatform)
    override val defaultPlatform get() = CommonPlatforms.defaultCommonPlatform

    override val argumentsClass get() = K2MetadataCompilerArguments::class.java

    override val name get() = "Common (experimental)"
}

val IdePlatformKind<*>?.isCommon
    get() = this is CommonIdePlatformKind