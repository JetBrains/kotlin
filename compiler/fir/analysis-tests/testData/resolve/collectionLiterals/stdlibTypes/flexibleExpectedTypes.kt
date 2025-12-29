// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

// FILE: Utils.java

import java.util.List;
import java.util.Set;

public class Utils {
    static <T> void acceptList(List<T> lst) {
    }

    static <T> void acceptArray(T[] arr) {
    }

    static <T> void acceptSet(Set<T> st) {
    }

    static void acceptListString(List<String> lst) {
    }

    static void acceptIntArray(int[] arr) {
    }
}

// FILE: main.kt

fun test() {
    Utils.<!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    Utils.<!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    Utils.<!CANNOT_INFER_PARAMETER_TYPE!>acceptArray<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    Utils.acceptArray([1, 2, 3])
    Utils.<!CANNOT_INFER_PARAMETER_TYPE!>acceptSet<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    Utils.<!CANNOT_INFER_PARAMETER_TYPE!>acceptSet<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    Utils.acceptListString(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    Utils.acceptListString(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    Utils.acceptListString(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["42"]<!>)
    Utils.acceptIntArray([])
    Utils.acceptIntArray([1, 2, 3])
    Utils.acceptIntArray([<!ARGUMENT_TYPE_MISMATCH!>"42"<!>])
}

/* GENERATED_FIR_TAGS: collectionLiteral, flexibleType, functionDeclaration, integerLiteral, javaFunction, stringLiteral */
