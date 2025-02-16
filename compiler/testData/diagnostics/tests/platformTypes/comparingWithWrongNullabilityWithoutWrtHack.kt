// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK
// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN +DontMakeExplicitJavaTypeArgumentsFlexible

import java.util.Comparator;

fun foo() {
    Comparator.comparing<String?, Boolean?> {
        it != ""
    }
}
