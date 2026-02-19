// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-57502

import kotlin.properties.Delegates

fun test(foo: Any) {
    var test by Delegates.observable(true) { property, oldValue, newValue ->  }
    test = foo is String
    if (test) {
        foo.<!UNRESOLVED_REFERENCE!>length<!> // no smartcast
    }
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, ifExpression, isExpression, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, propertyDelegate, setter, starProjection */
