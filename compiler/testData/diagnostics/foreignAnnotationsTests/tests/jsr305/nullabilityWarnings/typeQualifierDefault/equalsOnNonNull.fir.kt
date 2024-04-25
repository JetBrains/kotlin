// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: warn

// FILE: spr/NonNullApi.java
package spr;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER})
public @interface NonNullApi {
}

// FILE: A.java
import spr.*;

@NonNullApi
public class A {
    public String getBar() {
        return "";
    }
}

// FILE: main.kt
fun main(a: A) {
    if (a.getBar() == "1" && a.hashCode() != 0) return
    if (a.getBar() != "2" && a.hashCode() != 0) return

    if (a.getBar() == null && a.hashCode() != 0) return
    if (a.getBar() != null && a.hashCode() != 0) return

    if (a.bar == "1" && a.hashCode() != 0) return
    if (a.bar != "2" && a.hashCode() != 0) return

    if (a.bar == null && a.hashCode() != 0) return
    if (a.bar != null && a.hashCode() != 0) return
}
