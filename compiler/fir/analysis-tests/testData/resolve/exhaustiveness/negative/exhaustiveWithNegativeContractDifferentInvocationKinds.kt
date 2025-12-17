// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +DataFlowBasedExhaustiveness
// WITH_STDLIB

import kotlin.contracts.*

/* GENERATED_FIR_TAGS: andExpression, assignment, callableReference, classReference, contractCallsEffect, contracts,
enumDeclaration, enumEntry, equalityExpression, functionDeclaration, functionalType, ifExpression, integerLiteral,
interfaceDeclaration, isExpression, lambdaLiteral, lateinit, localProperty, nestedClass, nullableType, objectDeclaration,
propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */
enum class MyEnum { A, B, C }
sealed interface MySealedInterface {
    object A : MySealedInterface
    object B : MySealedInterface
    object C : MySealedInterface
}

@OptIn(ExperimentalContracts::class)
fun myRunExactlyOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun exactlyOnceEnum(x0: MyEnum): Int {
    var x: MyEnum = x0
    if (x == MyEnum.A) return 1
    if (x == MyEnum.B) return 2
    myRunExactlyOnce {
        when (x) {
            MyEnum.C -> return@myRunExactlyOnce
        }
        x = MyEnum.A
    }

    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (x) {
        MyEnum.A -> 2
        MyEnum.C -> 4
    }<!>
}

fun exactlyOnceSealed(x0: MySealedInterface?): Int {
    var x: MySealedInterface? = x0
    requireNotNull(x)
    myRunExactlyOnce {
        <!NO_ELSE_IN_WHEN!>when<!> (x) {
            is MySealedInterface.B -> return@myRunExactlyOnce
            is MySealedInterface.C -> return@myRunExactlyOnce
        }
        x = MySealedInterface.A
    }

    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        MySealedInterface.A -> 2
        MySealedInterface.B -> 3
        MySealedInterface.C -> 4
    }
}

@OptIn(ExperimentalContracts::class)
fun myRunAtMostOnce(block: () -> Unit, shouldRun: Boolean) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (shouldRun) {
        block()
    }
}

fun atMostOnceEnum(x0: MyEnum, shouldRun: Boolean): Int {
    var x: MyEnum = x0
    if (x == MyEnum.A) return 1
    if (x == MyEnum.B) return 2
    myRunAtMostOnce({
        when (x) {
            MyEnum.C -> return@myRunAtMostOnce
        }
                        x = MyEnum.A
                    }, shouldRun)
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (x) {
        MyEnum.A -> 2
        MyEnum.C -> 4
    }<!>
}

@OptIn(ExperimentalContracts::class)
fun myRunUnknown(block: () -> Unit) {
    block()
}

fun unknownEnum(x0: MyEnum): Int {
    var x: MyEnum = x0
    if (x == MyEnum.A) return 1
    if (x == MyEnum.C) return 2

    myRunUnknown {
        <!NO_ELSE_IN_WHEN!>when<!> (x) {
            MyEnum.B -> return@myRunUnknown
        }
        x = MyEnum.A
    }

    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        MyEnum.A -> 2
        MyEnum.B -> 3
    }
}

lateinit var y: MySealedInterface
fun lateinitUnknown(yInitial: MySealedInterface?) {
    if (yInitial != null) {
        y = yInitial
    }
}

fun test(): Int {
    myRunUnknown {
        if (::y.isInitialized && y is MySealedInterface.C) {
            return@myRunUnknown
        }
    }
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (y) {
        is MySealedInterface.A -> 1
        is MySealedInterface.B -> 2
        is MySealedInterface.C -> 3
    }<!>
}

@OptIn(ExperimentalContracts::class)
fun myRunAtLeastOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    block()
}

fun atLeastOnceSealed(x0: MySealedInterface?): Int {
    var x: MySealedInterface? = x0
    requireNotNull(x)

    myRunAtLeastOnce {
        <!NO_ELSE_IN_WHEN!>when<!> (x) {
            is MySealedInterface.B -> return@myRunAtLeastOnce
            is MySealedInterface.C -> return@myRunAtLeastOnce
        }
        x = MySealedInterface.A
    }

    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        MySealedInterface.A -> 2
        MySealedInterface.B -> 3
        MySealedInterface.C -> 4
    }
}
