// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK

import java.util.Comparator;

fun foo() {
    <!TYPE_MISMATCH!>Comparator.comparing<String?, <!UPPER_BOUND_VIOLATED!>Boolean?<!>> {
        it != ""
    }<!>
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, functionDeclaration, inProjection, javaFunction, lambdaLiteral,
nullableType, outProjection, samConversion, stringLiteral */
