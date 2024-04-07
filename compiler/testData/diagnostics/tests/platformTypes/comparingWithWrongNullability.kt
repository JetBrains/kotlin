// FIR_IDENTICAL
// FULL_JDK
// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN

import java.util.Comparator;

fun foo() {
    Comparator.comparing<String?, Boolean?> {
        it != ""
    }
}
