// TARGET_BACKEND: JVM_IR
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


// FILE: box.kt

fun foo(arg: @Nls(capitalization = Nls.Capitalization.Sentence) String) = arg

fun box() = foo("OK")