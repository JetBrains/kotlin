// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// SKIP_JAVAC
// FILE: p/Nameless.java

package p;

public class Nameless {
    void () {}
    int ;
}

// FILE: k.kt

import p.*

class K : Nameless() {
    <!FUNCTION_DECLARATION_WITH_NO_NAME!>fun ()<!> {}
    val<!SYNTAX!><!> : Int = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, javaType, propertyDeclaration */
