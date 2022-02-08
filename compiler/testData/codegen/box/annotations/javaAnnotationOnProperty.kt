// WITH_REFLECT
// TARGET_BACKEND: JVM
// FILE: Ann1.java
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Ann1 {}

// FILE: Ann2.java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Ann2 {}

// FILE: box.kt
class C {
    @Ann1 @Ann2 val x = 1
}

fun box(): String {
    require(C::class.java.getDeclaredField("x")?.getAnnotation(Ann1::class.java) != null) { "no Ann1 on field x" }
    require(C::class.java.getDeclaredMethod("getX\$annotations")?.getAnnotation(Ann2::class.java) != null) { "no Ann2 on property x" }
    return "OK"
}
