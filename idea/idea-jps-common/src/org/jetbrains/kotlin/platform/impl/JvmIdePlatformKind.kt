/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("JvmIdePlatformUtil")
package org.jetbrains.kotlin.platform.impl

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.IdePlatform
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

object JvmIdePlatformKind : IdePlatformKind<JvmIdePlatformKind>() {

    override fun platformByCompilerArguments(arguments: CommonCompilerArguments): IdePlatform<JvmIdePlatformKind, CommonCompilerArguments>? {
        return if (arguments is K2JVMCompilerArguments) {
            val jvmTarget = arguments.jvmTarget ?: JvmTarget.DEFAULT.description
            JvmIdePlatformKind.platforms.firstOrNull { it.version.description >= jvmTarget }
        } else null
    }

    override val compilerPlatform get() = JvmPlatform

    override val platforms = JvmTarget.values().map { ver -> Platform(ver) }
    override val defaultPlatform get() = Platform(JvmTarget.JVM_1_6)

    override val argumentsClass get() = K2JVMCompilerArguments::class.java

    override val name get() = "JVM"

    data class Platform(override val version: JvmTarget) : IdePlatform<JvmIdePlatformKind, K2JVMCompilerArguments>() {
        override val kind get() = JvmIdePlatformKind

        override fun createArguments(init: K2JVMCompilerArguments.() -> Unit) = K2JVMCompilerArguments()
            .apply(init)
            .apply { jvmTarget = this@Platform.version.description }
    }
}

val IdePlatformKind<*>?.isJvm
    get() = this is JvmIdePlatformKind

val IdePlatform<*, *>?.isJvm
    get() = this is JvmIdePlatformKind.Platform