// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

// FILE: A.java
import org.checkerframework.checker.nullness.compatqual.*;

public class A {
    @NullableDecl public String field = null;

    @NullableDecl
    public String foo(@NonNullDecl String x, @NullableDecl CharSequence y) {
        return "";
    }

    @NonNullDecl
    public String bar() {
        return "";
    }

}

// FILE: main.kt
fun main(a: A) {
    a.foo("", null)?.length
    a.foo("", null)<!UNSAFE_CALL!>.<!>length
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>, "")<!UNSAFE_CALL!>.<!>length

    a.bar().length
    a.bar()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length

    a.field?.length
    a.field<!UNSAFE_CALL!>.<!>length
}
