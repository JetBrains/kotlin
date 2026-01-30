// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// FULL_JDK

import java.util.Comparator;

fun foo() {
    Comparator.comparing<String?, <!UPPER_BOUND_VIOLATED!>Boolean?<!>> <!ARGUMENT_TYPE_MISMATCH!>{
        it != ""
    }<!>
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, functionDeclaration, inProjection, javaFunction, lambdaLiteral,
outProjection, samConversion, stringLiteral */
