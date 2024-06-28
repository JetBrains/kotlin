// FIR_IDENTICAL
// FILE: J.java
import kotlin.DeprecationLevel;

public class J {
    @Deprecated
    @kotlin.Deprecated(message = "", level = DeprecationLevel.HIDDEN)
    public void javaDeprecatedKotlinHidden() {
    }

    @Deprecated
    @kotlin.Deprecated(message = "", level = DeprecationLevel.WARNING)
    public void javaDeprecatedKotlinWarning() {
    }

    @Deprecated
    @kotlin.Deprecated(message = "", level = DeprecationLevel.ERROR)
    public void javaDeprecatedKotlinError() {
    }

    @kotlin.Deprecated(message = "", level = DeprecationLevel.HIDDEN)
    public void kotlinHidden() {
    }

    @kotlin.Deprecated(message = "", level = DeprecationLevel.WARNING)
    public void kotlinWarning() {
    }

    @kotlin.Deprecated(message = "", level = DeprecationLevel.ERROR)
    public void kotlinError() {
    }
}

// FILE:test.kt
fun test(j: J) {
    j.<!UNRESOLVED_REFERENCE!>javaDeprecatedKotlinHidden<!>()
    j.<!DEPRECATION!>javaDeprecatedKotlinWarning<!>()
    j.<!DEPRECATION_ERROR!>javaDeprecatedKotlinError<!>()
    j.<!UNRESOLVED_REFERENCE!>kotlinHidden<!>()
    j.<!DEPRECATION!>kotlinWarning<!>()
    j.<!DEPRECATION_ERROR!>kotlinError<!>()
}