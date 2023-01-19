// one.KotlinFacadeKt
// SKIP_IDE_TEST

// FILE: KotlinFacade.kt
package one

import one.JavaClass.staticJavaMethod

private val MY_FIELD = "abcd"

// FILE: one/JavaClass.java
package one;

import two.Service;

import static one.KotlinFacadeKt.*;

@Service(Service.Level.PROJECT)
public final class JavaClass {
    static void staticJavaMethod() {

    }
}

// FILE: two/Service.java
package two;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
    Level[] value() default {};

    enum Level {
        PROJECT
    }
}
