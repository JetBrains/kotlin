// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-25808
// WITH_STDLIB

// FILE: B.java
public class B {
}

// FILE: kt25808.kt
// KT-25808: False negative "Operator '==' cannot be applied to" for platform types
class A

fun main(args: Array<String>) {
    (1 to A()) == A() // should produce EQUALITY_NOT_APPLICABLE
    (1 to B()) == B() // false negative: no error for platform type
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, integerLiteral, javaFunction, javaType */
