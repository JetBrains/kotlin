// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS
//           (^ changes positioning of `WRONG_NUMBER_OF_TYPE_ARGUMENTS`)
// LANGUAGE: +CompanionBlocksAndExtensions
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
    G::foo
    GtoG::bar
    NG::foo
    GtoNG::bar
    NGtoG::foo

    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>G<*><!>::bar
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>GtoG<Any><!>::foo
    <!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>GtoNG<Nothing><!>::foo

    // wrong number
    NG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::foo
    NGtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*><!>::foo
    G<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::bar
    GtoG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>::foo
    GtoNG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Any, Nothing><!>::bar
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaCallableReference, nullableType, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
