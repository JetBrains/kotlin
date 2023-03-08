/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBinaryExpression


public interface KtAssignmentNamesProviderMixIn : KtAnalysisSessionMixIn {

    public fun KtBinaryExpression.getNames(): Collection<Name> {
        return analysisSession.assignmentNamesProvider.getNames(this)
    }
}


public abstract class KtAssignmentNamesProvider : KtAnalysisSessionComponent() {
    public abstract fun getNames(expression: KtBinaryExpression): Collection<Name>
}