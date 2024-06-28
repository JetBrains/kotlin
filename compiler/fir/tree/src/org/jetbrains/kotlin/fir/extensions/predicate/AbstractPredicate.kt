/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions.predicate

import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.fir.extensions.FirPredicateBasedProvider

/**
 * Predicates are the mechanism for compiler plugins which allows to search for annotated declarations
 *   or check if some declaration matches some predicate or not.
 * There are two different kinds of predicates: [LookupPredicate] and [DeclarationPredicate]
 *
 * [LookupPredicate] allows user to get all declarations in current module, which matches this predicate
 * Both [LookupPredicate] and [DeclarationPredicate] allow user to check if some declaration matches the predicate
 * The main component which can be used with predicate is [FirPredicateBasedProvider]
 *
 * The main difference between [LookupPredicate] and [DeclarationPredicate] is that [DeclarationPredicate] allows
 *   to create predicate with meta annotations, and [LookupPredicate] allows to use only annotations with predefined
 *   qualified names
 *
 * Note that predicates can not be used for matching or looking up local declarations (local functions, local classes, anonymous objects
 *   and their members). The only exception is matching local classes/anonymous objects with [AbstractPredicate.AnnotatedWith] and
 *   [AbstractPredicate.MetaAnnotatedWith] by [FirPredicateBasedProvider.matches]
 */
sealed interface AbstractPredicate<P : AbstractPredicate<P>> {
    val annotations: Set<AnnotationFqn>
    val metaAnnotations: Set<AnnotationFqn>

    fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R

    /**
     * Boolean combinator OR for two predicates, matches declaration if
     *   [a] matches declaration or [b] matches declaration
     */
    sealed interface Or<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        val a: P
        val b: P

        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitOr(this, data)
        }
    }

    /**
     * Boolean combinator AND for two predicates, matches declaration if
     *   [a] matches declaration and [b] matches declaration
     */
    sealed interface And<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        val a: P
        val b: P

        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAnd(this, data)
        }
    }

    // ------------------------------------ Annotated ------------------------------------

    /**
     * Base class for all predicates with specific annotations
     *  Declaration will be matched if at least one of [annotations] is found
     */
    sealed interface Annotated<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAnnotated(this, data)
        }
    }

    /**
     * Matches declarations, which are annotated with [annotations]
     *
     * @Ann
     * fun foo() {}
     *
     * fun bar(@Ann param: Int) {}
     *
     * @Ann
     * class A {
     *      fun baz() {}
     *
     *      class Nested {
     *          fun foobar() {}
     *      }
     * }
     *
     * Matched symbols: [fun foo, parameter `param` from fun bar, class A]
     */
    sealed interface AnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAnnotatedWith(this, data)
        }
    }

    /**
     * Matches declaration, if one of its containers annotated with [annotations]
     *
     * @Ann
     * fun foo() {}
     *
     * fun bar(@Ann param: Int) {}
     *
     * @Ann
     * class A {
     *      fun baz() {}
     *
     *      class Nested {
     *          fun foobar() {}
     *      }
     * }
     *
     * Matched symbols: [fun A.baz, class Nested, fun Nested.foobar]
     */
    sealed interface AncestorAnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAncestorAnnotatedWith(this, data)
        }
    }

    /**
     * Matches declaration, if its direct container annotated with [annotations]
     *
     * @Ann
     * fun foo() {}
     *
     * fun bar(@Ann param: Int) {}
     *
     * @Ann
     * class A {
     *      fun baz() {}
     *
     *      class Nested {
     *          fun foobar() {}
     *      }
     * }
     *
     * Matched symbols: [fun A.baz, class Nested]
     */
    sealed interface ParentAnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitParentAnnotatedWith(this, data)
        }
    }

    /**
     * Matches declaration, if one of its direct child declarations annotated with [annotations]
     *
     * @Ann
     * fun foo() {}
     *
     * fun bar(@Ann param: Int) {}
     *
     * class A {
     *      @Ann
     *      fun baz() {}
     *
     *      class Nested {
     *          fun foobar() {}
     *      }
     * }
     *
     * Matched symbols: [fun bar, class A]
     */
    sealed interface HasAnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitHasAnnotatedWith(this, data)
        }
    }

    // ------------------------------------ MetaAnnotated ------------------------------------

    /**
     * Matches declarations, which are annotated with annotations which are annotated with [metaAnnotations]
     *
     * [includeItself] flag determines if declaration, annotated with meta-annotation itself will
     *   be considered as matching to predicate
     *
     * Relation "annotation with meta annotation" is transitive. E.g. in snippet below some declaration will
     *   be matched with predicate MetaAnnotatedWith("Ann") if it is annotated with `@Ann` (if [includeItself] set to true),
     *   `@Some` or `@Other`
     *
     * @Ann
     * annotation class Some
     *
     * @Some
     * annotation class Other
     *
     *
     * @Some
     * fun foo() {}
     *
     * fun bar(@Ann param: Int) {}
     *
     * @Some
     * class A {
     *      fun baz() {}
     *
     *      class Nested {
     *          @Other
     *          fun foobar() {}
     *      }
     * }
     *
     * Matched symbols: [fun foo, class A, fun A.Nested.foobar]
     *
     * Note that [MetaAnnotatedWith] predicate has no implementation in [LookupPredicate] hierarchy
     *   and can not be used for global lookup
     */
    sealed interface MetaAnnotatedWith<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        val includeItself: Boolean

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
