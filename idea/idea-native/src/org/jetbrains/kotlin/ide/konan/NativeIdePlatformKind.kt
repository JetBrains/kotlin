/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.TargetPlatformVersion
import org.jetbrains.kotlin.platform.IdePlatform
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform

object NativeIdePlatformKind : IdePlatformKind<NativeIdePlatformKind>() {

    override fun platformByCompilerArguments(arguments: CommonCompilerArguments): IdePlatform<NativeIdePlatformKind, CommonCompilerArguments>? {
        return if (arguments is FakeK2NativeCompilerArguments) Platform
        else null
    }

    override val compilerPlatform get() = KonanPlatform

    override val platforms get() = listOf(Platform)
    override val defaultPlatform get() = Platform

    override val argumentsClass get() = FakeK2NativeCompilerArguments::class.java

    override val name get() = "Native"

    object Platform : IdePlatform<NativeIdePlatformKind, FakeK2NativeCompilerArguments>() {
        override val kind get() = NativeIdePlatformKind
        override val version get() = TargetPlatformVersion.NoVersion
        override fun createArguments(init: FakeK2NativeCompilerArguments.() -> Unit) = FakeK2NativeCompilerArguments().apply(init)
    }
}

// These are fake compiler arguments for Kotlin/Native - only for usage within IDEA plugin:
class FakeK2NativeCompilerArguments : CommonCompilerArguments()

