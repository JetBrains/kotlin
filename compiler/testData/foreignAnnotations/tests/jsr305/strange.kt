// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// FILE: A.java

import javax.annotation.*;
import javax.annotation.meta.*;

public class A {
    @Nonnull(when=When.UNKNOWN) public String field = null;

    @Nonnull(when=When.MAYBE)
    public String foo(@Nonnull(when=When.ALWAYS) String x, @Nonnull(when=When.NEVER) CharSequence y) {
        return "";
    }

    @Nonnull
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
    a.field.length
}
