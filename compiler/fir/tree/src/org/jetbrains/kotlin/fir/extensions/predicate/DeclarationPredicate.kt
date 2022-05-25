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

// ------------------------------------ Annotated ------------------------------------

/**
 * Base class for all predicates with specific annotations
 *  Declaration will be matched if at least one of [annotations] is found
 */
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
class AnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations) {
    override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
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
class UnderAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations) {
    override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
        return visitor.visitUnderAnnotatedWith(this, data)
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
class ParentAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations) {
    override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
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

class HasAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations) {
    override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
        return visitor.visitHasAnnotatedWith(this, data)
    }
}

// ------------------------------------ MetaAnnotated ------------------------------------

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

class ParentMetaAnnotatedWith(metaAnnotations: Set<AnnotationFqn>) : MetaAnnotated(metaAnnotations) {
    override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
        return visitor.visitParentMetaAnnotatedWith(this, data)
    }
}

class HasMetaAnnotatedWith(metaAnnotations: Set<AnnotationFqn>) : MetaAnnotated(metaAnnotations) {
    override fun <R, D> accept(visitor: DeclarationPredicateVisitor<R, D>, data: D): R {
        return visitor.visitHasMetaAnnotatedWith(this, data)
    }
}

// -------------------------------------------- DSL --------------------------------------------

infix fun DeclarationPredicate.or(other: DeclarationPredicate): DeclarationPredicate = DeclarationPredicate.Or(this, other)
infix fun DeclarationPredicate.and(other: DeclarationPredicate): DeclarationPredicate = DeclarationPredicate.And(this, other)

// ------------------- varargs -------------------
fun under(vararg annotations: AnnotationFqn): DeclarationPredicate = UnderAnnotatedWith(annotations.toSet())
fun annotated(vararg annotations: AnnotationFqn): DeclarationPredicate = AnnotatedWith(annotations.toSet())
fun parentAnnotated(vararg annotations: AnnotationFqn): DeclarationPredicate = ParentAnnotatedWith(annotations.toSet())
fun hasAnnotated(vararg annotations: AnnotationFqn): DeclarationPredicate = HasAnnotatedWith(annotations.toSet())

fun metaUnder(vararg metaAnnotations: AnnotationFqn): DeclarationPredicate = UnderMetaAnnotated(metaAnnotations.toSet())
fun metaAnnotated(vararg metaAnnotations: AnnotationFqn): DeclarationPredicate = AnnotatedWithMeta(metaAnnotations.toSet())
fun parentMetaAnnotated(vararg metaAnnotations: AnnotationFqn): DeclarationPredicate = ParentMetaAnnotatedWith(metaAnnotations.toSet())
fun hasMetaAnnotated(vararg metaAnnotations: AnnotationFqn): DeclarationPredicate = HasMetaAnnotatedWith(metaAnnotations.toSet())

fun annotatedOrUnder(vararg annotations: AnnotationFqn): DeclarationPredicate = annotated(*annotations) or under(*annotations)
fun metaAnnotatedOrUnder(vararg metaAnnotations: AnnotationFqn): DeclarationPredicate =
    metaAnnotated(*metaAnnotations) or metaUnder(*metaAnnotations)

// ------------------- collections -------------------
fun under(annotations: Collection<AnnotationFqn>): DeclarationPredicate = UnderAnnotatedWith(annotations.toSet())
fun annotated(annotations: Collection<AnnotationFqn>): DeclarationPredicate = AnnotatedWith(annotations.toSet())
fun parentAnnotated(annotations: Collection<AnnotationFqn>): DeclarationPredicate = ParentAnnotatedWith(annotations.toSet())
fun hasAnnotated(annotations: Collection<AnnotationFqn>): DeclarationPredicate = HasAnnotatedWith(annotations.toSet())

fun metaUnder(metaAnnotations: Collection<AnnotationFqn>): DeclarationPredicate = UnderMetaAnnotated(metaAnnotations.toSet())
fun metaAnnotated(metaAnnotations: Collection<AnnotationFqn>): DeclarationPredicate = AnnotatedWithMeta(metaAnnotations.toSet())
fun parentMetaAnnotated(metaAnnotations: Collection<AnnotationFqn>): DeclarationPredicate = ParentMetaAnnotatedWith(metaAnnotations.toSet())
fun hasMetaAnnotated(metaAnnotations: Collection<AnnotationFqn>): DeclarationPredicate = HasMetaAnnotatedWith(metaAnnotations.toSet())

fun annotatedOrUnder(annotations: Collection<AnnotationFqn>): DeclarationPredicate = annotated(annotations) or under(annotations)
fun metaAnnotatedOrUnder(metaAnnotations: Collection<AnnotationFqn>): DeclarationPredicate =
    metaAnnotated(metaAnnotations) or metaUnder(metaAnnotations)
