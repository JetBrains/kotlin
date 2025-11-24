// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface Base
interface Derived : Base

@OptIn(ExperimentalContracts::class)
inline fun <T : Base, R> T.myUse(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return block(this)
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Derived, R> T.myUse(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return block(this)
}

class A(d: Derived) {
    private val urls: String
    init {
        d.myUse { urls = "" }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, classReference, contractCallsEffect, contracts,
funWithExtensionReceiver, functionDeclaration, functionalType, init, inline, interfaceDeclaration, lambdaLiteral,
nullableType, primaryConstructor, propertyDeclaration, stringLiteral, thisExpression, typeConstraint, typeParameter */
