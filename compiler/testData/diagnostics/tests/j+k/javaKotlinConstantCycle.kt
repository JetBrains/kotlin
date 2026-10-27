// RUN_PIPELINE_TILL: FRONTEND
// USE_PSI_JAVA_FACADE: false
// ISSUE: KT-90790
// MUTE_LL_FIR: KT-90993 StackOverflowError: symbol light class constant evaluation recurses through the PSI Java constant evaluator

// FILE: p/Greeter.java
package p;

public interface Greeter {
    int A = Constants.B + 1;
}

// FILE: test.kt
package p

object Constants {
    const val B = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>Greeter.A<!> + 1
}

/* GENERATED_FIR_TAGS: additiveExpression, const, integerLiteral, javaProperty, objectDeclaration, propertyDeclaration */
