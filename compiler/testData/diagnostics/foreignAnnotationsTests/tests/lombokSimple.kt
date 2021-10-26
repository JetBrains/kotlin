// FIR_IDENTICAL
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
    <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>a.foo("")<!UNNECESSARY_SAFE_CALL!>?.<!>length<!>
    <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)<!UNNECESSARY_SAFE_CALL!>?.<!>length<!>
}
