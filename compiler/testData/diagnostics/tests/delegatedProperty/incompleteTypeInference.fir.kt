// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A

class D {
    val c: Int <!DELEGATION_OPERATOR_MISSING!>by<!> IncorrectThis<A>()
}

val cTopLevel: Int <!DELEGATION_OPERATOR_MISSING!>by<!> IncorrectThis<A>()

class IncorrectThis<T> {
    fun <R> get(t: Any?, p: KProperty<*>): Int {
        return 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, propertyDeclaration,
propertyDelegate, starProjection, typeParameter */
