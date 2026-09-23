// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

// FILE: J.java
import kotlin.WillBecomeValue;

@WillBecomeValue
public class J {}

// FILE: main.kt

fun main() {
    <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>J()<!> === <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>J()<!>
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, javaFunction, javaType */
