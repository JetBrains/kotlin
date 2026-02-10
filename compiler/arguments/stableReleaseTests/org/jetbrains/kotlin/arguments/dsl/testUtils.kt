/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl

import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.stable.dsl.base.KotlinCompilerArgument as StableKotlinCompilerArgument
import org.jetbrains.kotlin.arguments.stable.dsl.base.KotlinReleaseVersion as StableKotlinReleaseVersion
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

private val temporaryExceptions = setOf("Xuse-javac", "Xcompile-java", "Xjavac-arguments")

internal fun Set<StableKotlinCompilerArgument>.filterNonDeprecated() = filter {
    it.isObsolete || it.releaseVersionsMetadata.deprecatedVersion != null
}

internal val StableKotlinReleaseVersion.asCurrent: KotlinReleaseVersion
    get() = KotlinReleaseVersion.entries.single { it.releaseName == releaseName }

internal val ClosedRange<StableKotlinReleaseVersion>.asCurrent: ClosedRange<KotlinReleaseVersion>
    get() = start.asCurrent..endInclusive.asCurrent

internal fun getSuperclassGenericType(kClass: KClass<*>): Class<*>? {
    val genericSuperclass = kClass.java.genericSuperclass
    return if (genericSuperclass is ParameterizedType) {
        genericSuperclass.actualTypeArguments[0] as? Class<*>
    } else null
}