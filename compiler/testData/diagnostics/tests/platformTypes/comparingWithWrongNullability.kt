// FIR_IDENTICAL
// FULL_JDK

import java.util.Comparator;

fun foo() {
    Comparator.comparing<String?, Boolean?> {
        it != ""
    }
}
