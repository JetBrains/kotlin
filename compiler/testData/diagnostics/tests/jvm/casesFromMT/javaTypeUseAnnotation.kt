// RUN_PIPELINE_TILL: BACKEND
// FILE: TypeAnno.java
import java.lang.annotation.*;

@Target({ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeAnno {
    String value() default "";
}

// FILE: J.java
// Type-use annotation on a parameter and return position. JavaTypeConversion.kt's
// filterTypeUseAnnotations callback decides which annotations propagate to the type
// (TYPE_USE only) vs to the declaration. Without filtering, declaration-only annotations
// can leak into types, or TYPE_USE annotations are dropped from types.
public class J {
    public static @TypeAnno("ret") String foo(@TypeAnno("p") String s) {
        return s == null ? "null" : s;
    }
}

// FILE: useSite.kt
fun bar(): String = J.foo("ok")

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, stringLiteral */
