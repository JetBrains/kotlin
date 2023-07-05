/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions.predicate

abstract class PredicateVisitor<P : AbstractPredicate<P>, R, D> {
    abstract fun visitPredicate(predicate: AbstractPredicate<P>, data: D): R

    open fun visitAnd(predicate: AbstractPredicate.And<P>, data: D): R {
        return visitPredicate(predicate, data)
    }

    open fun visitOr(predicate: AbstractPredicate.Or<P>, data: D): R {
        return visitPredicate(predicate, data)
    }

    open fun visitAnnotated(predicate: AbstractPredicate.Annotated<P>, data: D): R {
        return visitPredicate(predicate, data)
    }

    open fun visitAnnotatedWith(predicate: AbstractPredicate.AnnotatedWith<P>, data: D): R {
        return visitAnnotated(predicate, data)
    }

    open fun visitAncestorAnnotatedWith(predicate: AbstractPredicate.AncestorAnnotatedWith<P>, data: D): R {
        return visitAnnotated(predicate, data)
    }

    open fun visitParentAnnotatedWith(predicate: AbstractPredicate.ParentAnnotatedWith<P>, data: D): R {
        return visitAnnotated(predicate, data)
    }

    open fun visitHasAnnotatedWith(predicate: AbstractPredicate.HasAnnotatedWith<P>, data: D): R {
        return visitAnnotated(predicate, data)
    }

    open fun visitMetaAnnotatedWith(predicate: AbstractPredicate.MetaAnnotatedWith<P>, data: D): R {
        return visitPredicate(predicate, data)
    }
}

// It can be used externally by plugins
@Suppress("unused")
typealias DeclarationPredicateVisitor<R, D> = PredicateVisitor<DeclarationPredicate, R, D>
// It can be used externally by plugins
@Suppress("unused")
typealias LookupPredicateVisitor<R, D> = PredicateVisitor<LookupPredicate, R, D>
