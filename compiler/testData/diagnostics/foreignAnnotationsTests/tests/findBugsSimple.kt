// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

// FILE: A.java
import edu.umd.cs.findbugs.annotations.*;

public class A {
    @Nullable public String field = null;

    @PossiblyNull
    public String foo(@NonNull String x, @UnknownNullness CharSequence y) {
        return "";
    }

    @NonNull
    public String bar() {
        return "";
    }
}

// FILE: main.kt
fun main(a: A) {
    a.foo("", null)?.length
    a.foo("", null)<!UNSAFE_CALL!>.<!>length
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>, "")<!UNSAFE_CALL!>.<!>length
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>, null)<!UNSAFE_CALL!>.<!>length

    a.bar().length
    a.bar()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length

    a.field?.length
    a.field<!UNSAFE_CALL!>.<!>length
}
