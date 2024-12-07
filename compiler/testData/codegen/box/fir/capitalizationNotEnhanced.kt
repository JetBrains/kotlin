// TARGET_BACKEND: JVM_IR
// FULL_JDK
// JVM_TARGET: 1.8
// MODULE: m1
// FILE: Nls.java

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE, ElementType.TYPE, ElementType.PACKAGE})
public @interface Nls {

    enum Capitalization {
        NotSpecified,
        Title,
        Sentence
    }

    Capitalization capitalization() default Capitalization.NotSpecified;
}

// FILE: Bundle.java

import java.util.function.Supplier;

public class Bundle {
    public static Supplier<@Nls String> pointer() {
        return null;
    }
}

// MODULE: m2(m1)
// FILE: some.kt

import java.util.function.Supplier
import Nls.Capitalization.Title

object Bar {
    fun foo(s: Supplier<@Nls(capitalization = Title) String>?) {}
}

// MODULE: m3(m2, m1)
// FILE: box.kt

fun box(): String {
    Bar.foo(Bundle.pointer())
    return "OK"
}
