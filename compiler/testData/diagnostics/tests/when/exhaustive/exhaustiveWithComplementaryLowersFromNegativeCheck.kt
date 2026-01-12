// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78068, KT-78071, KT-78072
// LANGUAGE: +DataFlowBasedExhaustiveness

import kotlin.contracts.*

enum class MyEnum { A, B, C }

fun negSimpleEnum(x: MyEnum): Int {
    if (x != MyEnum.C) return 0

    return <!WHEN_ON_SEALED!>when (x) {
        MyEnum.C -> 3
    }<!>
}

fun simpleVar(i: MyEnum): Int {
    var x: MyEnum = i
    if (x == MyEnum.A) return 1
    x = MyEnum.A

    return <!WHEN_ON_SEALED!>when (x) {
        MyEnum.A -> 2
    }<!>
}

@OptIn(ExperimentalContracts::class)
fun myRunExactlyOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun exactlyOnceEnum(i: MyEnum): Int {
    var x: MyEnum = i
    if (x == MyEnum.A) return 1
    if (x == MyEnum.B) return 2

    myRunExactlyOnce {
        when (x) {
            MyEnum.C -> return@myRunExactlyOnce
        }
        x = MyEnum.A
    }

    return <!WHEN_ON_SEALED!>when (x) {
        MyEnum.A -> 3
        MyEnum.C -> 4
    }<!>
}

fun exactlyOnceEnum2(i: MyEnum): Int {
    var x: MyEnum = i

    myRunExactlyOnce {
        x = MyEnum.B
    }

    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        MyEnum.A -> 3
        MyEnum.C -> 4
    }
}

/* GENERATED_FIR_TAGS: assignment, classReference, contractCallsEffect, contracts, enumDeclaration, enumEntry,
equalityExpression, functionDeclaration, functionalType, ifExpression, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, smartcast, whenExpression, whenWithSubject */
