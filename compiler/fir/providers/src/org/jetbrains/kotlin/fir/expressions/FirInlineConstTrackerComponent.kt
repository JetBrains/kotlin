/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirEvaluatorResult
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.unwrapOr
import org.jetbrains.kotlin.incremental.components.InlineConstTracker

open class FirInlineConstTrackerComponent(val inlineConstTracker: InlineConstTracker?) : FirSessionComponent {
    object Default : FirInlineConstTrackerComponent(null)

    fun report(field: FirField, file: FirFile?, result: FirEvaluatorResult) {
        if (inlineConstTracker == null) return
        if (field.origin !is FirDeclarationOrigin.Java) return

        val filePath = file?.sourceFile?.path ?: return
        val owner = field.containingClassLookupTag()?.classId?.asString()
            ?.replace(".", "$")?.replace("/", ".")
            ?: return
        val evaluatedConstant = result.unwrapOr<FirLiteralExpression> { return } ?: return
        inlineConstTracker.report(
            filePath = filePath,
            owner = owner,
            name = field.name.asString(),
            constType = evaluatedConstant.kind.asString
        )

    }
}

val FirSession.inlineConstTracker: FirInlineConstTrackerComponent by FirSession.sessionComponentAccessor<FirInlineConstTrackerComponent>()
