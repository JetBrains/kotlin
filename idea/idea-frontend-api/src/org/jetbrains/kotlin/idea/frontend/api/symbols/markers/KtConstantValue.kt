/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols.markers

import org.jetbrains.kotlin.types.ConstantValueKind

public sealed class KtConstantValue
public object KtUnsupportedConstantValue : KtConstantValue()

public data class KtSimpleConstantValue<T>(val constantValueKind: ConstantValueKind<T>, val value: T) : KtConstantValue()