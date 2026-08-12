// RUN_PIPELINE_TILL: BACKEND
// FILE: Provider.java

public class Provider {
    public static Boolean getCondition() {
        return null;
    }
}

// FILE: main.kt

fun test_1(): Int = <!WHEN_SUBJECT_CAN_BE_NULL_IN_JAVA!>when<!> (Provider.getCondition()) {
    true -> 1
    false -> 2
}

fun test_2(): Int = when (Provider.getCondition()) {
    true -> 1
    false -> 2
    null -> 3
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, integerLiteral, whenExpression, whenWithSubject */
