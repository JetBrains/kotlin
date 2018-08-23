/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

import org.jetbrains.kotlin.ApplicationExtensionDescriptor
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.resolve.TargetPlatform

abstract class IdePlatformKind<Kind : IdePlatformKind<Kind>> {
    abstract val compilerPlatform: TargetPlatform
    abstract val platforms: List<IdePlatform<Kind, *>>

    abstract val defaultPlatform: IdePlatform<Kind, *>

    abstract val argumentsClass: Class<out CommonCompilerArguments>

    abstract val name: String

    override fun equals(other: Any?): Boolean = javaClass == other?.javaClass
    override fun hashCode(): Int = javaClass.hashCode()

    companion object : ApplicationExtensionDescriptor<IdePlatformKind<*>>(
        "org.jetbrains.kotlin.idePlatformKind", IdePlatformKind::class.java
    ) {
        val ALL_KINDS by lazy { getInstances() }
        val All_PLATFORMS by lazy { ALL_KINDS.flatMap { it.platforms } }
    }
}

fun IdePlatformKind<*>?.orDefault(): IdePlatformKind<*> {
    return this ?: DefaultIdeTargetPlatformKindProvider.defaultPlatform.kind
}

fun IdePlatform<*, *>?.orDefault(): IdePlatform<*, *> {
    return this ?: DefaultIdeTargetPlatformKindProvider.defaultPlatform
}