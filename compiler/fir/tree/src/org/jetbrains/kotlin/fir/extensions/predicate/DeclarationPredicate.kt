/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions.predicate

import org.jetbrains.kotlin.fir.extensions.AnnotationFqn

// -------------------------------------------- Predicates --------------------------------------------

/**
 * For reference read KDoc to [AbstractPredicate]
 * @see [AbstractPredicate]
 */
sealed class DeclarationPredicate : AbstractPredicate<DeclarationPredicate> {
    abstract override val annotations: Set<AnnotationFqn>
    abstract override val metaAnnotations: Set<AnnotationFqn>

    abstract override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R

    class Or(
        override val a: DeclarationPredicate,
        override val b: DeclarationPredicate
    ) : DeclarationPredicate(), AbstractPredicate.Or<DeclarationPredicate> {
        override val annotations: Set<AnnotationFqn> = a.annotations + b.annotations
        override val metaAnnotations: Set<AnnotationFqn> = a.metaAnnotations + b.metaAnnotations

        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitOr(this, data)
        }
    }

    class And(
        override val a: DeclarationPredicate,
        override val b: DeclarationPredicate
    ) : DeclarationPredicate(), AbstractPredicate.And<DeclarationPredicate> {
        override val annotations: Set<AnnotationFqn> = a.annotations + b.annotations
        override val metaAnnotations: Set<AnnotationFqn> = a.metaAnnotations + b.metaAnnotations

        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitAnd(this, data)
        }
    }

    // ------------------------------------ Annotated ------------------------------------

    sealed class Annotated(final override val annotations: Set<AnnotationFqn>) : DeclarationPredicate(),
        AbstractPredicate.Annotated<DeclarationPredicate> {
        init {
            require(annotations.isNotEmpty()) {
                "Annotations should be not empty"
            }
        }

        final override val metaAnnotations: Set<AnnotationFqn>
            get() = emptySet()

        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitAnnotated(this, data)
        }
    }

    class AnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations), AbstractPredicate.AnnotatedWith<DeclarationPredicate> {
        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitAnnotatedWith(this, data)
        }
    }

    class AncestorAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations),
        AbstractPredicate.AncestorAnnotatedWith<DeclarationPredicate> {
        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitAncestorAnnotatedWith(this, data)
        }
    }

    class ParentAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations),
        AbstractPredicate.ParentAnnotatedWith<DeclarationPredicate> {
        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitParentAnnotatedWith(this, data)
        }
    }

    class HasAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations),
        AbstractPredicate.HasAnnotatedWith<DeclarationPredicate> {
        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitHasAnnotatedWith(this, data)
        }
    }

    // ------------------------------------ MetaAnnotated ------------------------------------

    class MetaAnnotatedWith(
        override val metaAnnotations: Set<AnnotationFqn>,
        override val includeItself: Boolean
    ) : DeclarationPredicate(), AbstractPredicate.MetaAnnotatedWith<DeclarationPredicate> {
        init {
            require(metaAnnotations.isNotEmpty()) {
                "Annotations should be not empty"
            }
        }

        override val annotations: Set<AnnotationFqn>
            get() = emptySet()

        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitMetaAnnotatedWith(this, data)
        }
    }

    // -------------------------------------------- DSL --------------------------------------------

    object BuilderContext : AbstractPredicate.BuilderContext<DeclarationPredicate>() {
        override infix fun DeclarationPredicate.or(other: DeclarationPredicate): DeclarationPredicate = Or(this, other)
        override infix fun DeclarationPredicate.and(other: DeclarationPredicate): DeclarationPredicate = And(this, other)

        // ------------------- varargs -------------------
        override fun annotated(vararg annotations: AnnotationFqn): DeclarationPredicate = annotated(annotations.toList())
        override fun ancestorAnnotated(vararg annotations: AnnotationFqn): DeclarationPredicate = ancestorAnnotated(annotations.toList())
        override fun parentAnnotated(vararg annotations: AnnotationFqn): DeclarationPredicate = parentAnnotated(annotations.toList())
        override fun hasAnnotated(vararg annotations: AnnotationFqn): DeclarationPredicate = hasAnnotated(annotations.toList())

        override fun annotatedOrUnder(vararg annotations: AnnotationFqn): DeclarationPredicate =
            annotated(*annotations) or ancestorAnnotated(*annotations)

        fun metaAnnotated(vararg metaAnnotations: AnnotationFqn, includeItself: Boolean): DeclarationPredicate =
            MetaAnnotatedWith(metaAnnotations.toSet(), includeItself)

        // ------------------- collections -------------------
        override fun annotated(annotations: Collection<AnnotationFqn>): DeclarationPredicate = AnnotatedWith(annotations.toSet())
        override fun ancestorAnnotated(annotations: Collection<AnnotationFqn>): DeclarationPredicate =
            AncestorAnnotatedWith(annotations.toSet())

        override fun parentAnnotated(annotations: Collection<AnnotationFqn>): DeclarationPredicate =
            ParentAnnotatedWith(annotations.toSet())

        override fun hasAnnotated(annotations: Collection<AnnotationFqn>): DeclarationPredicate = HasAnnotatedWith(annotations.toSet())

        override fun annotatedOrUnder(annotations: Collection<AnnotationFqn>): DeclarationPredicate =
            annotated(annotations) or ancestorAnnotated(annotations)

        fun metaAnnotated(metaAnnotations: Collection<AnnotationFqn>, includeItself: Boolean): DeclarationPredicate =
            MetaAnnotatedWith(metaAnnotations.toSet(), includeItself)
    }

    companion object {
        inline fun create(init: BuilderContext.() -> DeclarationPredicate): DeclarationPredicate = BuilderContext.init()
    }
}
