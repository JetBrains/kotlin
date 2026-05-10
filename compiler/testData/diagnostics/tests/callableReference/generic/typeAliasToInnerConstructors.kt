// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS
//           (^ changes positioning of `WRONG_NUMBER_OF_TYPE_ARGUMENTS`)
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE: +ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

// FILE: p1/file1.kt

package p1

class WithInner<K> {
    inner class Inner
}

typealias TA = WithInner<Int>.Inner
typealias GTA<K> = WithInner<K>.Inner

typealias NG = WithInner<Int>
typealias G<K> = WithInner<K>

class WithTA {
    typealias NestedTA = WithInner<Int>.Inner
}

fun test() {
    WithInner<Int>::TA
    WithInner<Int>::GTA
    G<Int>::TA
    G<String>::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>TA<!>
    NG::TA
    NG::GTA
    G<Int>::GTA

    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>WithInner<!>::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>TA<!>
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>WithInner<!>::GTA
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>TA<!>
    <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>::GTA

    <!CANNOT_INFER_PARAMETER_TYPE!>with<!>(WithInner<Int>()) {
        WithTA::<!UNRESOLVED_REFERENCE!>NestedTA<!>
    }

    // wrong number of type arguments
    G<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::TA
    NG<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>::TA
    WithInner<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::TA
    WithInner<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::GTA
}

// FILE: p2/file2.kt

package p2

import p1.WithTA.NestedTA

fun test() {
    p1.WithInner<Int>::NestedTA
    p1.G<Int>::NestedTA
    p1.NG::NestedTA
    p1.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>WithInner<!>::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>NestedTA<!>
    p1.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>G<!>::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>NestedTA<!>
}

/* GENERATED_FIR_TAGS: callableReference, capturedType, classDeclaration, functionDeclaration, inner, lambdaLiteral,
nullableType, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
