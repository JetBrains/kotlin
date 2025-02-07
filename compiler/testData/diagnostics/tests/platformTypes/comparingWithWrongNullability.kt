// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FIR_DUMP
// FULL_JDK
// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN

import java.util.Comparator;

fun foo() {
    Comparator.comparing<String?, Boolean?> {
        it != ""
    }
}
