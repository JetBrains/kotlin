// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78071
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +DataFlowBasedExhaustiveness

import kotlin.contracts.*

enum class MyEnum { A, B, C }

sealed interface MySealedInterface {
    object A : MySealedInterface
    object B : MySealedInterface
    object C : MySealedInterface
}

var e: MyEnum = MyEnum.A

fun whenOnVar(): Int {
    return when (e) {
        MyEnum.A -> 1
        MyEnum.B -> 2
        MyEnum.C -> 3
    }
}

open class OpenEnum {
    open val e: MyEnum = MyEnum.A
}

fun whenOnOpen(o: OpenEnum): Int {
    return when (o.e) {
        MyEnum.A -> 1
        MyEnum.B -> 2
        MyEnum.C -> 3
    }
}

fun simpleVarMutation(initial: MyEnum): Int {
    var x: MyEnum = initial
    if (x == MyEnum.A) return 1
    x = MyEnum.A
    return when (x) { // KT-78071
        MyEnum.A -> 2
    }
}

fun simpleSealedVarMutation(initial: MySealedInterface): Int {
    var x: MySealedInterface = initial
    if (x is MySealedInterface.A) return 1
    x = MySealedInterface.A
    return when (x) {
        <!USELESS_IS_CHECK!>is MySealedInterface.A<!> -> 2
    }
}

fun varMultiMutation(initial: MyEnum): Int {
    var x: MyEnum = initial
    if (x == MyEnum.A) return 1
    if (x == MyEnum.B) {
        x = MyEnum.C
    }
    if (x == MyEnum.C) {
        x = MyEnum.A
    }
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        MyEnum.A -> 2
    }
}

@OptIn(ExperimentalContracts::class)
fun fSealedVarExactlyOnceShort(initial: MySealedInterface?): Int {
    var x: MySealedInterface? = initial
    <!UNRESOLVED_REFERENCE!>requireNotNull<!>(x)
    if (x is MySealedInterface.A) return 1

    <!UNRESOLVED_REFERENCE!>myRunExactlyOnce<!> {
        <!NO_ELSE_IN_WHEN!>when<!> (x) {
            is MySealedInterface.B -> return@myRunExactlyOnce
            is MySealedInterface.C -> return@myRunExactlyOnce
        }
        x = MySealedInterface.A
    }
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is MySealedInterface.A -> 2
        is MySealedInterface.B -> 3
        is MySealedInterface.C -> 4
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, classReference, enumDeclaration, enumEntry, equalityExpression,
functionDeclaration, ifExpression, integerLiteral, interfaceDeclaration, isExpression, lambdaLiteral, localProperty,
nestedClass, nullableType, objectDeclaration, propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */
