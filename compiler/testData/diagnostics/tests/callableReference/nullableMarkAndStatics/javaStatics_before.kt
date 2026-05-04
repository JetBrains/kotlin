// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -CompanionBlocksAndExtensions
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
    C?::foo
    TAtoC?::bar
    TAtoNC::foo
    TAtoNC?::bar
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaCallableReference, javaType, nullableType, typeAliasDeclaration */
