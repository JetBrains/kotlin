// FILE: androidx/annotation/RecentlyNonNull.java
package androidx.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.TYPE})
public @interface RecentlyNonNull {
}

// FILE: androidx/annotation/A.java
package androidx.annotation;

public class A {
    @RecentlyNonNull
    public String bar(@RecentlyNonNull String string) {
        return "";
    }
}

// FILE: main.kt
import androidx.annotation.A

fun test(a: A) = a.b<caret><caret_onAirContext>ar()