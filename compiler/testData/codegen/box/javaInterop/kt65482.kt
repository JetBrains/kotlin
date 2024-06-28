// WITH_STDLIB
// TARGET_BACKEND: JVM

// FILE: MyApi.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({
    ElementType.FIELD,
    ElementType.METHOD,
    ElementType.TYPE,
    ElementType.CONSTRUCTOR,
    ElementType.PACKAGE
})
public @interface MyApi {
    Integer LATEST = -1;
    long value();
}

// FILE: main.kt

fun box(): String {
    if (-1 == MyApi.LATEST) return "OK"
    return "NOT OK"
}
