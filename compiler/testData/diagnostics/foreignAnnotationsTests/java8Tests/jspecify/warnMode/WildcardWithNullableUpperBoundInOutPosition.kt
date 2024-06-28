// JSPECIFY_STATE: warn
// NULLABILITY_ANNOTATIONS: @org.jetbrains.annotations:warn
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-66946

// FILE: Ann.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
public @interface Ann {}

// FILE: FromJava.java

import org.jspecify.annotations.*;

@NullMarked
public class FromJava<T extends @Nullable Object> {
    public T produce() { return null; }

    public static FromJava<? extends @Nullable Object> EXPLICIT_UPPER_BOUND = new FromJava<@Nullable Object>();
    public static FromJava<? extends @Nullable @Ann Object> EXPLICIT_UPPER_BOUND2 = new FromJava<@Nullable Object>();
    public static FromJava<? extends @org.jetbrains.annotations.Nullable Object> EXPLICIT_UPPER_BOUND3 = new FromJava<@Nullable Object>();
    public static FromJava<? super @Nullable Object> EXPLICIT_LOWER_BOUND = new FromJava<@Nullable Object>();
    public static FromJava<?> IMPLICIT_BOUNDS = new FromJava<@Nullable Object>();
}

// FILE: kotlin.kt
fun <T> accept(arg: T) {}

fun test() {
    accept<Any?>(FromJava.EXPLICIT_UPPER_BOUND.produce())
    accept<Any>(FromJava.EXPLICIT_UPPER_BOUND.produce())
    accept<Any>(FromJava.EXPLICIT_UPPER_BOUND2.produce())
    accept<Any>(FromJava.EXPLICIT_UPPER_BOUND3.produce())

    accept<Any?>(FromJava.EXPLICIT_LOWER_BOUND.produce())
    accept<Any>(<!TYPE_MISMATCH!>FromJava.EXPLICIT_LOWER_BOUND.produce()<!>)

    accept<Any?>(FromJava.IMPLICIT_BOUNDS.produce())
    accept<Any>(FromJava.IMPLICIT_BOUNDS.produce())
}
