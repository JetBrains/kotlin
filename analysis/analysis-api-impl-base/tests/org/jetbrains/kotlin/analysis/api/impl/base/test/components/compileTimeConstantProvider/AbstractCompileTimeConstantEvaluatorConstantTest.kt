/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.components.compileTimeConstantProvider

import org.jetbrains.kotlin.analysis.api.components.KtConstantEvaluationMode

abstract class AbstractCompileTimeConstantEvaluatorConstantTest
    : AbstractCompileTimeConstantEvaluatorTest(KtConstantEvaluationMode.CONSTANT_EXPRESSION_EVALUATION)
