/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("CommonIdePlatformUtil")
package org.jetbrains.kotlin.platform.impl

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms
import org.jetbrains.kotlin.resolve.TargetPlatform

object CommonIdePlatformKind : IdePlatformKind<CommonIdePlatformKind>() {

    override fun platformByCompilerArguments(arguments: CommonCompilerArguments): TargetPlatform? {
        return if (arguments is K2MetadataCompilerArguments)
            DefaultBuiltInPlatforms.commonPlatform
        else
            null
    }

    override fun createArguments(): CommonCompilerArguments {
        return K2MetadataCompilerArguments() // TODO(dsavvinov): review that, as now MPP !== K2Metadata
    }

    override val platforms get() = listOf(DefaultBuiltInPlatforms.commonPlatform)
    override val defaultPlatform get() = DefaultBuiltInPlatforms.commonPlatform

    override val argumentsClass get() = K2MetadataCompilerArguments::class.java

    override val name get() = "Common (experimental)"
}

val IdePlatformKind<*>?.isCommon
    get() = this is CommonIdePlatformKind