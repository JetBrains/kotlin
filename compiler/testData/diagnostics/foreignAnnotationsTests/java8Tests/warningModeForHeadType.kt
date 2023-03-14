// FIR_IDENTICAL
// FULL_JDK
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

// FILE: ElementTypesAreNonnullByDefault.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@TypeQualifierDefault({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Nonnull
@interface ElementTypesAreNonnullByDefault {
}

// FILE: Maps.java
import org.checkerframework.checker.nullness.qual.Nullable;
// Here it's important that @ElementTypesAreNonnullByDefault is a JSR-305 default qualifier and disabled by default (resulting in warnings-only)
// Thus return type (head type) is considered as warningly-annotated as not-nullable and that makes annotations on bounds for K and V
// be effectively ignored on non-warnings level.
@ElementTypesAreNonnullByDefault
public final class Maps {
    public static <K extends @Nullable Object, V extends @Nullable Object> java.util.HashMap<K,V> newHashMap() { return null; }
}

// FILE: main.kt

fun foo() {
    val x = Maps.newHashMap<String, Int>()
    x.put("", 1)
    // If there were no @ElementTypesAreNonnullByDefault on the Maps class, there would be an error on `null` argument because the type of `x`
    // would be `HashMap<String, Int>!`, i.e. with non-flexible type arguments, thus not allowing nulls.
    x.put("", null)
}
