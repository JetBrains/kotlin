/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions.predicate

import org.jetbrains.kotlin.fir.extensions.AnnotationFqn

sealed interface AbstractPredicate<P : AbstractPredicate<P>> {
    val annotations: Set<AnnotationFqn>
    val metaAnnotations: Set<AnnotationFqn>

    fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R

    sealed interface Or<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        val a: P
        val b: P

        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitOr(this, data)
        }
    }

    sealed interface And<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        val a: P
        val b: P

        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAnd(this, data)
        }
    }

    // ------------------------------------ Annotated ------------------------------------

    sealed interface Annotated<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAnnotated(this, data)
        }
    }

    sealed interface AnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAnnotatedWith(this, data)
        }
    }

    sealed interface AncestorAnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAncestorAnnotatedWith(this, data)
        }
    }


    sealed interface ParentAnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitParentAnnotatedWith(this, data)
        }
    }

    sealed interface HasAnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitHasAnnotatedWith(this, data)
        }
    }

    // ------------------------------------ MetaAnnotated ------------------------------------

    sealed interface MetaAnnotatedWith<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitMetaAnnotatedWith(this, data)
        }
    }

    // -------------------------------------------- DSL --------------------------------------------

    abstract class BuilderContext<P : AbstractPredicate<P>> {
        abstract infix fun P.or(other: P): P
        abstract infix fun P.and(other: P): P

        // ------------------- varargs -------------------
        abstract fun annotated(vararg annotations: AnnotationFqn): P
        abstract fun ancestorAnnotated(vararg annotations: AnnotationFqn): P
        abstract fun parentAnnotated(vararg annotations: AnnotationFqn): P
        abstract fun hasAnnotated(vararg annotations: AnnotationFqn): P

        abstract fun annotatedOrUnder(vararg annotations: AnnotationFqn): P

        // ------------------- collections -------------------
        abstract fun annotated(annotations: Collection<AnnotationFqn>): P
        abstract fun ancestorAnnotated(annotations: Collection<AnnotationFqn>): P
        abstract fun parentAnnotated(annotations: Collection<AnnotationFqn>): P
        abstract fun hasAnnotated(annotations: Collection<AnnotationFqn>): P

        abstract fun annotatedOrUnder(annotations: Collection<AnnotationFqn>): P
    }
}
