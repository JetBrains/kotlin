// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FILE: StringHolder.java

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StringHolder {
    public String value();
}

// FILE: fileFacade.kt

@file:StringHolder("OK")

fun box(): String =
        Class.forName("FileFacadeKt").getAnnotation(StringHolder::class.java)?.value ?: "null"
