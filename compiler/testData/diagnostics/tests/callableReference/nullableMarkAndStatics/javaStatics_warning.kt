// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE: -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

// FILE: test/C.java
package test;

public class C {
    public static void foo() { }
    public static int bar = 42;
}

// FILE: test/Test.kt

package test

typealias TAtoC = C
typealias TAtoNC = C?

fun test() {
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>C<!>?::foo
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>TAtoC<!>?::bar
    TAtoNC::foo
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>TAtoNC<!>?::bar
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaCallableReference, javaType, nullableType, typeAliasDeclaration */
