// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: p/Value.java
package p;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Value {
    int value() default /* default */ 7;
}

// FILE: p/Explicit.java
package p;

@Value(value = /* before */ 1 /* around plus */ + 2 /* after */)
public class Explicit {}

// FILE: p/Defaulted.java
package p;

@Value
public class Defaulted {}

// FILE: test.kt
fun box(): String {
    val explicit = p.Explicit::class.java.getAnnotation(p.Value::class.java)?.value
    val defaulted = p.Defaulted::class.java.getAnnotation(p.Value::class.java)?.value
    return if (explicit == 3 && defaulted == 7) "OK" else "FAIL: explicit=$explicit defaulted=$defaulted"
}