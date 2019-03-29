/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.resolve.TargetPlatformVersion
import org.jetbrains.kotlin.utils.DescriptionAware

abstract class IdePlatform<Kind : IdePlatformKind<Kind>, out Arguments : CommonCompilerArguments> : DescriptionAware {
    abstract val kind: Kind
    abstract val version: TargetPlatformVersion

    abstract fun createArguments(init: Arguments.() -> Unit = {}): Arguments

    override val description
        get() = kind.name + " " + version.description

    override fun toString() = description
}
