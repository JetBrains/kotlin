/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object RequireKotlinConstants {
    val FQ_NAME: FqName = FqName("kotlin.internal.RequireKotlin")

    val VERSION: Name = Name.identifier("version")
    val MESSAGE: Name = Name.identifier("message")
    val LEVEL: Name = Name.identifier("level")
    val VERSION_KIND: Name = Name.identifier("versionKind")
    val ERROR_CODE: Name = Name.identifier("errorCode")

    val VERSION_REGEX: Regex = "(0|[1-9][0-9]*)".let { number -> Regex("$number\\.$number(\\.$number)?") }
}
