// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK
// LANGUAGE: +DontMakeExplicitJavaTypeArgumentsFlexible

import java.util.Comparator;

fun foo() {
    Comparator.comparing<String?, <!UPPER_BOUND_VIOLATED!>Boolean?<!>> <!ARGUMENT_TYPE_MISMATCH!>{
        it != ""
    }<!>
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, functionDeclaration, inProjection, javaFunction, lambdaLiteral,
nullableType, outProjection, samConversion, stringLiteral */
