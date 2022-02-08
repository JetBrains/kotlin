/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions.predicate

abstract class DeclarationPredicateVisitor<R, D> {
    abstract fun visitPredicate(predicate: DeclarationPredicate, data: D): R
    open fun visitAny(predicate: DeclarationPredicate.Any, data: D): R {
        return visitPredicate(predicate, data)
    }

    open fun visitAnd(predicate: DeclarationPredicate.And, data: D): R {
        return visitPredicate(predicate, data)
    }

    open fun visitOr(predicate: DeclarationPredicate.Or, data: D): R {
        return visitPredicate(predicate, data)
    }

    open fun visitAnnotated(predicate: Annotated, data: D): R {
        return visitPredicate(predicate, data)
    }

    open fun visitAnnotatedWith(predicate: AnnotatedWith, data: D): R {
        return visitAnnotated(predicate, data)
    }

    open fun visitUnderAnnotatedWith(predicate: UnderAnnotatedWith, data: D): R {
        return visitAnnotated(predicate, data)
    }

    open fun visitMetaAnnotated(predicate: MetaAnnotated, data: D): R {
        return visitPredicate(predicate, data)
    }

    open fun visitAnnotatedWithMeta(predicate: AnnotatedWithMeta, data: D): R {
        return visitMetaAnnotated(predicate, data)
    }

    open fun visitUnderMetaAnnotated(predicate: UnderMetaAnnotated, data: D): R {
        return visitMetaAnnotated(predicate, data)
    }
}
