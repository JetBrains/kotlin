/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("IdePlatformKindUtil")
package org.jetbrains.kotlin.platform

import org.jetbrains.kotlin.extensions.ApplicationExtensionDescriptor
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.isJps
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformKind
import org.jetbrains.kotlin.platform.impl.JsIdePlatformKind
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.platform.impl.NativeIdePlatformKind
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

abstract class IdePlatformKind<Kind : IdePlatformKind<Kind>> {
    abstract val compilerPlatform: TargetPlatform
    abstract val platforms: List<IdePlatform<Kind, *>>

    abstract val defaultPlatform: IdePlatform<Kind, *>

    abstract fun platformByCompilerArguments(arguments: CommonCompilerArguments): IdePlatform<Kind, CommonCompilerArguments>?

    abstract val argumentsClass: Class<out CommonCompilerArguments>

    abstract val name: String

    override fun equals(other: Any?): Boolean = javaClass == other?.javaClass
    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString() = name

    companion object {
        // We can't use the ApplicationExtensionDescriptor class directly because it's missing in the JPS process
        private val extension = run {
            if (isJps) return@run null
            ApplicationExtensionDescriptor("org.jetbrains.kotlin.idePlatformKind", IdePlatformKind::class.java)
        }

        // For using only in JPS
        private val JPS_KINDS
            get() = listOf(
                JvmIdePlatformKind,
                JsIdePlatformKind,
                CommonIdePlatformKind,
                NativeIdePlatformKind
            )

        val ALL_KINDS by lazy {
            val kinds = extension?.getInstances() ?: return@lazy JPS_KINDS
            require(kinds.isNotEmpty()) { "Platform kind list is empty" }
            kinds
        }

        val All_PLATFORMS by lazy { ALL_KINDS.flatMap { it.platforms } }

        val IDE_PLATFORMS_BY_COMPILER_PLATFORMS by lazy { ALL_KINDS.map { it.compilerPlatform to it }.toMap() }

        fun <Args : CommonCompilerArguments> platformByCompilerArguments(arguments: Args): IdePlatform<*, *>? =
            ALL_KINDS.firstNotNullResult { it.platformByCompilerArguments(arguments) }

    }
}

val TargetPlatform.idePlatformKind: IdePlatformKind<*>
    get() = IdePlatformKind.IDE_PLATFORMS_BY_COMPILER_PLATFORMS[this] ?: error("Unknown platform $this")

fun IdePlatformKind<*>?.orDefault(): IdePlatformKind<*> {
    return this ?: DefaultIdeTargetPlatformKindProvider.defaultPlatform.kind
}

fun IdePlatform<*, *>?.orDefault(): IdePlatform<*, *> {
    return this ?: DefaultIdeTargetPlatformKindProvider.defaultPlatform
}