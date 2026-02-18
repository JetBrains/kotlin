/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.resolution.KaVariableAccessCall
import org.jetbrains.kotlin.psi.KtExpression

@KaImplementationDetail
class KaBaseVariableWriteAccess(override val value: KtExpression?) : KaVariableAccessCall.Kind.Write,
    @Suppress("DEPRECATION") org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccess.Write
