// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +DataFlowBasedExhaustiveness
// WITH_STDLIB

sealed interface MySealedInterfaceDataClasses {
    data class Num(val n: Int) : MySealedInterfaceDataClasses
    object Zero : MySealedInterfaceDataClasses
    data class Str(val s: String) : MySealedInterfaceDataClasses
}

fun simpleGuard(e: MySealedInterfaceDataClasses) = <!NO_ELSE_IN_WHEN!>when<!> (e) {
    is MySealedInterfaceDataClasses.Num if e.n < 0 -> -1
    is MySealedInterfaceDataClasses.Num if e.n >= 0 -> 1
    is MySealedInterfaceDataClasses.Str -> 2
    MySealedInterfaceDataClasses.Zero -> 0
}

fun nullableGuard(x: String?) = <!NO_ELSE_IN_WHEN!>when<!> (x) {
    null -> 1
    <!USELESS_IS_CHECK!>is String<!> if x.isNotEmpty() -> 2
    <!USELESS_IS_CHECK!>is String<!> if x.isEmpty() -> 3

}

fun guardedAfterIf(e: MySealedInterfaceDataClasses) {
    if (e is MySealedInterfaceDataClasses.Num && e.n == 0) return
    <!NO_ELSE_IN_WHEN!>when<!> (e) {
        is MySealedInterfaceDataClasses.Num if e.n > 0 -> 1
        is MySealedInterfaceDataClasses.Num if e.n < 0 -> -1
        is MySealedInterfaceDataClasses.Str -> 2
        MySealedInterfaceDataClasses.Zero -> 0
    }
}

fun negGuard(e: MySealedInterfaceDataClasses) = <!WHEN_ON_SEALED_GEEN_ELSE!>when (e) {
    is MySealedInterfaceDataClasses.Num if !(e.n % 2 == 0) -> 0
    is MySealedInterfaceDataClasses.Num -> 1
    is MySealedInterfaceDataClasses.Str -> 2
    MySealedInterfaceDataClasses.Zero -> 0
}<!>

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, comparisonExpression, data, equalityExpression,
functionDeclaration, guardCondition, ifExpression, integerLiteral, interfaceDeclaration, isExpression,
multiplicativeExpression, nestedClass, nullableType, objectDeclaration, primaryConstructor, propertyDeclaration, sealed,
smartcast, whenExpression, whenWithSubject */
