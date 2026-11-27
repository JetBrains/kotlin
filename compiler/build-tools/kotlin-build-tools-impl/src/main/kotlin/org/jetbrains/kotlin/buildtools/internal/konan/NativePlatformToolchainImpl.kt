/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.konan

import org.jetbrains.kotlin.buildtools.api.arguments.enums.KonanTarget
import org.jetbrains.kotlin.buildtools.api.konan.NativeHost
import org.jetbrains.kotlin.buildtools.api.konan.NativePlatformToolchain
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.konan.operations.NativeKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.KonanTarget as InternalKonanTarget
import java.nio.file.Path

internal class NativePlatformToolchainImpl(private val compilerVersion: String) : NativePlatformToolchain {
    override fun nativeKlibCompilationOperationBuilder(
        sources: List<Path>,
        destination: Path,
    ): NativeKlibCompilationOperation.Builder =
        NativeKlibCompilationOperationImpl(
            sources,
            destination,
            compilerVersion = compilerVersion,
        )

    override val supportedHosts: List<NativeHost> get() = hosts
}

private class NativeHostImpl(
    override val target: KonanTarget,
    override val linkableTargets: Set<KonanTarget>,
) : NativeHost {
    override fun toString(): String = target.stringValue
}

private val commonTargets = setOf(
    InternalKonanTarget.LINUX_X64,
    InternalKonanTarget.LINUX_ARM32_HFP,
    InternalKonanTarget.LINUX_ARM64,
    InternalKonanTarget.MINGW_X64,
    InternalKonanTarget.ANDROID_X86,
    InternalKonanTarget.ANDROID_X64,
    InternalKonanTarget.ANDROID_ARM32,
    InternalKonanTarget.ANDROID_ARM64,
)

private val appleTargets = setOf(
    InternalKonanTarget.MACOS_ARM64,
    InternalKonanTarget.IOS_ARM64,
    InternalKonanTarget.IOS_X64,
    InternalKonanTarget.IOS_SIMULATOR_ARM64,
    InternalKonanTarget.WATCHOS_ARM64,
    InternalKonanTarget.WATCHOS_X64,
    InternalKonanTarget.WATCHOS_SIMULATOR_ARM64,
    InternalKonanTarget.WATCHOS_DEVICE_ARM64,
    InternalKonanTarget.TVOS_ARM64,
    InternalKonanTarget.TVOS_X64,
    InternalKonanTarget.TVOS_SIMULATOR_ARM64,
)

// This implementation is released together with the compiler, so the table describes exactly that compiler version.
// The targets are entries of the enum generated from the compiler arguments, so a removed target does not compile here.
private val hosts: List<NativeHost> = listOfNotNull(
    nativeHost(InternalKonanTarget.LINUX_X64, commonTargets),
    nativeHost(InternalKonanTarget.MINGW_X64, commonTargets),
    nativeHost(InternalKonanTarget.MACOS_ARM64, commonTargets + appleTargets),
)

private fun nativeHost(target: InternalKonanTarget, linkableTargets: Set<InternalKonanTarget>): NativeHost? =
    target.toApiOrNull()?.let { hostTarget ->
        NativeHostImpl(hostTarget, linkableTargets.mapNotNullTo(LinkedHashSet()) { it.toApiOrNull() })
    }

// Null for a target that the loaded API has no entry for yet. The API consumer could not name such a target anyway.
// `enumValues` and not `entries`: `entries` returns a kotlin-stdlib type, and the API is loaded by another class loader
// with its own kotlin-stdlib, so the call fails with a LinkageError.
@Suppress("EnumValuesSoftDeprecate")
private fun InternalKonanTarget.toApiOrNull(): KonanTarget? = enumValues<KonanTarget>().firstOrNull { it.name == name }
