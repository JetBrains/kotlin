// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-57649

open class A
abstract class B {
    fun test(current: A): A? =
        if (current === this) current else null
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, intersectionType,
nullableType, smartcast, thisExpression */
