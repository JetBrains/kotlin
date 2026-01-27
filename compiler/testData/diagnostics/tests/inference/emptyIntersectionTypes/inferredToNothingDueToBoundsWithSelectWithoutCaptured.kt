// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74999
// FIR_DUMP
// DUMP_INFERENCE_LOGS: FIXATION

interface SelfI<C : SelfI<C>>

class A<S : SelfI<S>> {
    fun <T : S> materialize(): T = Entity() <!UNCHECKED_CAST!>as T<!>
}

val aStar: A<*> = A()
val aEntity: A<Entity> = A()

interface Path : Traversable
val path = object : Path {}
interface Traversable

open class Entity : Traversable, SelfI<Entity>

fun main() {
//    when {
//        "".hashCode() > 0 -> path
//        else -> aStar.materialize()
//    } // Ok in K1 and 2.2.0
//
//    when {
//        "".hashCode() > 0 -> path
//        else -> aEntity.materialize()
//    } // Ok in K1, CCE in 2.2.0: class Entity cannot be cast to class Path

//    val x1 = when {
//        "".hashCode() > 0 -> path
//        else -> aStar.materialize()
//    } // Ok in 2.2.0, CCE in K1: class Entity cannot be cast to class Path

    val x2 = when {
        "".hashCode() > 0 -> path
        else -> aEntity.materialize()
    } // CCE both in K1 and K2: class Entity cannot be cast to class Path
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, asExpression, capturedType, classDeclaration, comparisonExpression,
functionDeclaration, integerLiteral, interfaceDeclaration, intersectionType, localProperty, propertyDeclaration,
starProjection, stringLiteral, typeConstraint, typeParameter, whenExpression */
