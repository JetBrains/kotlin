/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.regex.Pattern

internal val functionPattern = Pattern.compile("^K?(Suspend)?Function\\d+$")

internal val kotlinFqn = FqName("kotlin")
internal val kotlinCoroutinesFqn = kotlinFqn.child(Name.identifier("coroutines"))
internal val kotlinReflectFqn = kotlinFqn.child(Name.identifier("reflect"))

internal val functionalPackages = listOf(kotlinFqn, kotlinCoroutinesFqn, kotlinReflectFqn)