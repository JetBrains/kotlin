/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("NativeIdePlatformUtil")
package org.jetbrains.kotlin.platform.impl

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.KonanPlatforms

object NativeIdePlatformKind : IdePlatformKind<NativeIdePlatformKind>() {

    override fun platformByCompilerArguments(arguments: CommonCompilerArguments): TargetPlatform? {
        return if (arguments is FakeK2NativeCompilerArguments)
            KonanPlatforms.defaultKonanPlatform
        else
            null
    }

    override fun createArguments(): CommonCompilerArguments {
        return FakeK2NativeCompilerArguments()
    }

    override val defaultPlatform: TargetPlatform
        get() = KonanPlatforms.defaultKonanPlatform

    override val platforms
        get() = listOf(KonanPlatforms.defaultKonanPlatform)

    override val argumentsClass
        get() = FakeK2NativeCompilerArguments::class.java

    override val name
        get() = "Native"
}

// These are fake compiler arguments for Kotlin/Native - only for usage within IDEA plugin:
class FakeK2NativeCompilerArguments : CommonCompilerArguments()

val IdePlatformKind<*>?.isKotlinNative
    get() = this is NativeIdePlatformKind
