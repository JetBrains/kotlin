/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

@RequiresOptIn("Accessing `coneTypeOrNull` hides potential bugs if at the given point all expression and their types should be resolved. Consider using `resolvedType` instead.")
annotation class UnresolvedExpressionTypeAccess
