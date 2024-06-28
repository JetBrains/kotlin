/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.scopes

import org.jetbrains.kotlin.name.Name

@Deprecated("Use '(Name) -> Boolean' instead.", replaceWith = ReplaceWith("(Name) -> Boolean"))
public typealias KaScopeNameFilter = (Name) -> Boolean

@Deprecated("Use '(Name) -> Boolean' instead.", replaceWith = ReplaceWith("(Name) -> Boolean"))
public typealias KtScopeNameFilter = (Name) -> Boolean
