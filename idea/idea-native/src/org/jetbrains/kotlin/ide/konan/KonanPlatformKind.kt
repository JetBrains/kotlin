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

object KonanPlatformKind : IdePlatformKind<KonanPlatformKind>() {

    override val compilerPlatform get() = KonanPlatform

    // FIXME(ddol): implement support for multiple K/N targets
    override val platforms get() = listOf(Platform)
    override val defaultPlatform get() = Platform

    override val argumentsClass get() = KonanCompilerArguments::class.java

    override val name get() = "Native"

    object Platform : IdePlatform<KonanPlatformKind, KonanCompilerArguments>() {
        override val kind get() = KonanPlatformKind
        override val version get() = TargetPlatformVersion.NoVersion
        override fun createArguments(init: KonanCompilerArguments.() -> Unit) = KonanCompilerArguments().apply(init)
    }

    override fun equals(other: Any?): Boolean = other is KonanPlatformKind
    override fun hashCode(): Int = javaClass.hashCode()
}

// FIXME(ddol): implement support for K/N compiler arguments
class KonanCompilerArguments : CommonCompilerArguments()

val IdePlatformKind<*>?.isKonan
    get() = this is KonanPlatformKind

val IdePlatform<*, *>?.isKonan
    get() = this is KonanPlatformKind.Platform
