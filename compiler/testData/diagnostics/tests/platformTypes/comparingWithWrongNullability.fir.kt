// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// FULL_JDK
// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN -DontMakeExplicitJavaTypeArgumentsFlexible

import java.util.Comparator;

fun foo() {
    Comparator.comparing<String?, <!UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS("Comparable<in Boolean!>!; Boolean?")!>Boolean?<!>> {
        it != ""
    }
}
