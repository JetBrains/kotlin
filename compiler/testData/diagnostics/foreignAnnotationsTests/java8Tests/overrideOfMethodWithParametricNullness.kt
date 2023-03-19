// FIR_IDENTICAL
// FULL_JDK
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// ISSUE: KT-56656

// FILE: ParametricNullness.java
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
@Target({FIELD, METHOD, PARAMETER})
@javax.annotation.meta.TypeQualifierNickname
@javax.annotation.Nonnull(when = javax.annotation.meta.When.UNKNOWN)
@interface ParametricNullness {}

// FILE: ElementTypesAreNonnullByDefault.java
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

/**
 * Marks all "top-level" types as non-null in a way that is recognized by Kotlin. Note that this
 * unfortunately includes type-variable usages, so we also provide {@link ParametricNullness} to
 * "undo" it as best we can.
 */
@Retention(RUNTIME)
@Target(TYPE)
@TypeQualifierDefault({FIELD, METHOD, PARAMETER})
@Nonnull
@interface ElementTypesAreNonnullByDefault {}

// FILE: MyFunction.java
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@ElementTypesAreNonnullByDefault
public interface MyFunction<F extends @Nullable Object, T extends @Nullable Object> extends java.util.function.Function<F, T> {
    @Override
    @ParametricNullness
    T apply (@ParametricNullness F input);
}

// FILE: main.kt

class A : MyFunction<String?, String?> {
    override fun apply(x: String?): String? = ""
}
