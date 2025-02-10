// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// FULL_JDK
// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN -ForbidTypePreservingFlexibilityWriteInferenceHack

import java.util.Comparator;

fun foo() {
    Comparator.comparing<String?, Boolean?> {
        it != ""
    }
}
