// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS
//           (^ changes positioning of `WRONG_NUMBER_OF_TYPE_ARGUMENTS`)
// LANGUAGE: -CompanionBlocksAndExtensions
// LANGUAGE: -ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

// FILE: test/G.java
package test;

public class G<A> {
    public static void foo() { }
    public static int bar = 42;
}

// FILE: test/NG.java
package test;

public class NG {
    public static void foo() { }
    public static int bar = 42;
}

// FILE: test/Test.kt

package test

typealias GtoG<B> = G<B>
typealias GtoNG<C> = NG
typealias NGtoG = G<String>

fun test() {
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GtoG<!>::bar
    NG::foo
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>GtoNG<!>::bar
    NGtoG::foo

    G<*>::bar
    GtoG<Any>::foo
    GtoNG<Nothing>::foo

    // wrong number
    NG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::foo
    NGtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::foo
    G<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::bar
    GtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::foo
    GtoNG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Nothing><!>::bar
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaCallableReference, nullableType, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
