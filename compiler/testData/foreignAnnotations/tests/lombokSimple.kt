// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// FILE: A.java

import lombok.*;

public class A {
    @NonNull
    public String foo(@NonNull String x) {
        return "";
    }

}

// FILE: main.kt

fun main(a: A) {
    a.foo("").length
    a.foo("")<!UNNECESSARY_SAFE_CALL!>?.<!>length
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)<!UNNECESSARY_SAFE_CALL!>?.<!>length
}
