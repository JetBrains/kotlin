/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

import org.jetbrains.kotlin.analysis.api.symbols.markers.KtConstantValue
import org.jetbrains.kotlin.psi.KtExpression

public sealed class KtInitializerValue {
    public abstract val initializerPsi: KtExpression?
}

public class KtConstantInitializerValue(
    public val constant: KtConstantValue,
    override val initializerPsi: KtExpression?
) : KtInitializerValue()

public class KtNonConstantInitializerValue(
    override val initializerPsi: KtExpression?
) : KtInitializerValue()