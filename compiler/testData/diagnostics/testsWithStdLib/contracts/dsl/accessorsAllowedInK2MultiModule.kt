// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +AllowContractsOnPropertyAccessors
// Issue: KT-79506

// MODULE: m1
// FILE: declaration.kt
import kotlin.contracts.*

sealed class Status {
    class Ok : Status() {}
    class Error(val message: String) : Status()
}

val Status.isError: Boolean
    get() {
        contract { returns (true) implies (this@isError is Status.Error) }
        return this is Status.Error
    }

// FILE: usageSameModule.kt
fun testFromTheSameModule(status: Status) {
    if (status.isError) {
        status.message
    }
}

// MODULE: m2(m1)
// FILE: usageDifferentModule.kt
import Status
import isError

fun testFromOtherModule(status: Status) {
    if (status.isError) {
        status.message
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, contractConditionalEffect, contracts, functionDeclaration, getter, ifExpression,
isExpression, lambdaLiteral, nestedClass, primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver, sealed,
smartcast, thisExpression */
