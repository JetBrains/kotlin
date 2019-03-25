/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("JvmIdePlatformUtil")
package org.jetbrains.kotlin.platform.impl

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.resolve.JvmTarget
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms
import org.jetbrains.kotlin.resolve.TargetPlatform

object JvmIdePlatformKind : IdePlatformKind<JvmIdePlatformKind>() {

    override fun platformByCompilerArguments(arguments: CommonCompilerArguments): TargetPlatform? {
        if (arguments !is K2JVMCompilerArguments) return null

        val jvmTargetDescription = arguments.jvmTarget
            ?: return DefaultBuiltInPlatforms.jvmPlatform

        val jvmTarget = JvmTarget.values().firstOrNull { it.description >= jvmTargetDescription }
            ?: return DefaultBuiltInPlatforms.jvmPlatform

        return DefaultBuiltInPlatforms.jvmPlatformByTargetVersion(jvmTarget)
    }

    override fun createArguments(): CommonCompilerArguments {
        return K2JVMCompilerArguments()
    }

    override val platforms = JvmTarget.values().map { ver -> DefaultBuiltInPlatforms.jvmPlatformByTargetVersion(ver) }
    override val defaultPlatform get() = DefaultBuiltInPlatforms.jvmPlatform

    override val argumentsClass get() = K2JVMCompilerArguments::class.java

    override val name get() = "JVM"
}

val IdePlatformKind<*>?.isJvm
    get() = this is JvmIdePlatformKind
