// WITH_REFLECT
// TARGET_BACKEND: JVM
// FILE: Ann.java
import java.lang.annotation.*;

@Target({ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface Ann {}

// FILE: box.kt
fun box(): String {
    class C(val x: String, val y: String) {
        @Ann
        constructor(v: String): this(v, v)
    }

    require(C::class.java.getDeclaredConstructor(String::class.java).getAnnotation(Ann::class.java) != null) { "no Ann on constructor" }
    return "OK"
}
