/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions.predicate

import org.jetbrains.kotlin.fir.extensions.AnnotationFqn

// -------------------------------------------- Predicates --------------------------------------------

sealed class DeclarationPredicate {
    abstract val annotations: Set<AnnotationFqn>
    abstract val metaAnnotations: Set<AnnotationFqn>

    abstract fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R

    object Any : DeclarationPredicate() {
        override val annotations: Set<AnnotationFqn>
            get() = emptySet()
        override val metaAnnotations: Set<AnnotationFqn>
            get() = emptySet()

        override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
            return visitor.visitAny(this, data)
        }
    }

    class Or(val a: DeclarationPredicate, val b: DeclarationPredicate) : DeclarationPredicate() {
        override val annotations: Set<AnnotationFqn> = a.annotations + b.annotations
        override val metaAnnotations: Set<AnnotationFqn> = a.metaAnnotations + b.metaAnnotations

        override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
            return visitor.visitOr(this, data)
        }
    }

    class And(val a: DeclarationPredicate, val b: DeclarationPredicate) : DeclarationPredicate() {
        override val annotations: Set<AnnotationFqn> = a.annotations + b.annotations
        override val metaAnnotations: Set<AnnotationFqn> = a.metaAnnotations + b.metaAnnotations

        override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
            return visitor.visitAnd(this, data)
        }
    }
}

sealed class Annotated(final override val annotations: Set<AnnotationFqn>) : DeclarationPredicate() {
    init {
        require(annotations.isNotEmpty()) {
            "Annotations should be not empty"
        }
    }

    final override val metaAnnotations: Set<AnnotationFqn>
        get() = emptySet()

    override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
        return visitor.visitAnnotated(this, data)
    }
}

class AnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations) {
    override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
        return visitor.visitAnnotatedWith(this, data)
    }
}

class UnderAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations) {
    override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
        return visitor.visitUnderAnnotatedWith(this, data)
    }
}

// annotation class AllOpen  // from library

/*

@AllOpen
annotation class MyOpen

@MyOpen
class MyClass
 */

sealed class MetaAnnotated(final override val metaAnnotations: Set<AnnotationFqn>) : DeclarationPredicate() {
    init {
        require(metaAnnotations.isNotEmpty()) {
            "Annotations should be not empty"
        }
    }

    final override val annotations: Set<AnnotationFqn>
        get() = emptySet()

    override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
        return visitor.visitMetaAnnotated(this, data)
    }
}

class AnnotatedWithMeta(metaAnnotations: Set<AnnotationFqn>) : MetaAnnotated(metaAnnotations) {
    override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
        return visitor.visitAnnotatedWithMeta(this, data)
    }
}

class UnderMetaAnnotated(metaAnnotations: Set<AnnotationFqn>) : MetaAnnotated(metaAnnotations) {
    override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
        return visitor.visitUnderMetaAnnotated(this, data)
    }
}

// -------------------------------------------- DSL --------------------------------------------

infix fun DeclarationPredicate.or(other: DeclarationPredicate): DeclarationPredicate = DeclarationPredicate.Or(this, other)
infix fun DeclarationPredicate.and(other: DeclarationPredicate): DeclarationPredicate = DeclarationPredicate.And(this, other)

fun under(vararg annotations: AnnotationFqn): DeclarationPredicate = UnderAnnotatedWith(annotations.toSet())
fun has(vararg annotations: AnnotationFqn): DeclarationPredicate = AnnotatedWith(annotations.toSet())
fun metaUnder(vararg metaAnnotations: AnnotationFqn): DeclarationPredicate = UnderMetaAnnotated(metaAnnotations.toSet())
fun metaHas(vararg metaAnnotations: AnnotationFqn): DeclarationPredicate = AnnotatedWithMeta(metaAnnotations.toSet())

fun hasOrUnder(vararg annotations: AnnotationFqn): DeclarationPredicate = has(*annotations) or under(*annotations)
fun metaHasOrUnder(vararg metaAnnotations: AnnotationFqn): DeclarationPredicate = metaHas(*metaAnnotations) or metaUnder(*metaAnnotations)
