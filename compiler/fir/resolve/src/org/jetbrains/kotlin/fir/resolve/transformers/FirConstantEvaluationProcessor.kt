/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.evaluatedInitializer
import org.jetbrains.kotlin.fir.expressions.FirExpressionEvaluator
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping

@OptIn(AdapterForResolveProcessor::class)
class FirConstantEvaluationProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession, FirResolvePhase.CONSTANT_EVALUATION) {
    override val transformer = FirConstantEvaluationTransformerAdapter(session)
}

@AdapterForResolveProcessor
class FirConstantEvaluationTransformerAdapter(session: FirSession) : FirTransformer<Any?>() {
    private val transformer = FirConstantEvaluationBodyResolveTransformer(session)

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        error("Should only be called via transformFile()")
    }

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        return withFileAnalysisExceptionWrapping(file) {
            file.transform(transformer, null)
        }
    }
}

class FirConstantEvaluationBodyResolveTransformer(private val session: FirSession) : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        return element
    }

    override fun transformFile(file: FirFile, data: Nothing?): FirFile {
        return file.transformDeclarations(this, data)
    }

    override fun transformScript(script: FirScript, data: Nothing?): FirScript {
        return script.transformDeclarations(this, data)
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): FirStatement {
        return regularClass.transformDeclarations(this, data)
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): FirStatement {
        property.evaluatedInitializer = FirExpressionEvaluator.evaluatePropertyInitializer(property, session)
        return property
    }
}
