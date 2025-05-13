// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: -ForbidInferringTypeVariablesIntoEmptyIntersection
// ISSUES: KT-51357, KT-67146, KT-67335
// DUMP_CONSTRAINTS: MARKDOWN, MERMAID
class Expression<T>(val x: T)

class GreaterOp(val expr1: Expression<*>, val expr2: Expression<*>)

fun <T : Comparable<T>, S : T?> Expression<in S>.greater(other: T): GreaterOp =
    GreaterOp(this, Expression(other))

fun foo(countExpr: Expression<Long>) {
    countExpr.greater(0)
    countExpr.<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>greater<!>("0")
    countExpr.greater<String, Nothing>("0")
}
