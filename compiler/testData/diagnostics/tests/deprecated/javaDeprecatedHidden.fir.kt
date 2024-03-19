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
    j.<!DEPRECATION!>javaDeprecatedKotlinHidden<!>()
    j.<!DEPRECATION!>javaDeprecatedKotlinWarning<!>()
    j.<!DEPRECATION!>javaDeprecatedKotlinError<!>()
    j.<!UNRESOLVED_REFERENCE!>kotlinHidden<!>()
    j.<!DEPRECATION!>kotlinWarning<!>()
    j.<!DEPRECATION_ERROR!>kotlinError<!>()
}
