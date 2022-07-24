/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirWhenExpression : FirExpression(), FirResolvable {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract override val calleeReference: FirReference
    abstract val subject: FirExpression?
    abstract val subjectVariable: FirVariable?
    abstract val branches: List<FirWhenBranch>
    abstract val exhaustivenessStatus: ExhaustivenessStatus?
    abstract val usedAsExpression: Boolean


    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceCalleeReference(newCalleeReference: FirReference)

    abstract fun replaceSubject(newSubject: FirExpression?)

    abstract fun replaceSubjectVariable(newSubjectVariable: FirVariable?)

    abstract fun replaceBranches(newBranches: List<FirWhenBranch>)

    abstract fun replaceExhaustivenessStatus(newExhaustivenessStatus: ExhaustivenessStatus?)
}

inline fun <D> FirWhenExpression.transformTypeRef(transformer: FirTransformer<D>, data: D): FirWhenExpression 
     = apply { replaceTypeRef(typeRef.transform(transformer, data)) }

inline fun <D> FirWhenExpression.transformAnnotations(transformer: FirTransformer<D>, data: D): FirWhenExpression 
     = apply { replaceAnnotations(annotations.transform(transformer, data)) }

inline fun <D> FirWhenExpression.transformCalleeReference(transformer: FirTransformer<D>, data: D): FirWhenExpression 
     = apply { replaceCalleeReference(calleeReference.transform(transformer, data)) }

inline fun <D> FirWhenExpression.transformSubject(transformer: FirTransformer<D>, data: D): FirWhenExpression 
    = apply { if (subjectVariable != null) {
        replaceSubjectVariable(subjectVariable?.transform(transformer, data))
        replaceSubject(subjectVariable?.initializer)
       } else {
           replaceSubject(subject?.transform(transformer, data))
       }
       }

inline fun <D> FirWhenExpression.transformSubjectVariable(transformer: FirTransformer<D>, data: D): FirWhenExpression 
     = apply { replaceSubjectVariable(subjectVariable?.transform(transformer, data)) }

inline fun <D> FirWhenExpression.transformBranches(transformer: FirTransformer<D>, data: D): FirWhenExpression 
     = apply { replaceBranches(branches.transform(transformer, data)) }
