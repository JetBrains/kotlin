// WITH_STDLIB
// FILE: androidx/annotation/RecentlyNonNull.java
package androidx.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Retention(CLASS)
@Target({METHOD, PARAMETER, FIELD})
public @interface RecentlyNullable {
}

// FILE: androidx/annotation/A.java
package androidx.annotation;

public class A {
    @RecentlyNullable
    public String bar(@RecentlyNullable String string) {
        return "";
    }
}

// FILE: main.kt
import androidx.annotation.A

fun test(a: A) = <expr>a.bar()</expr>