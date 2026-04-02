// JSPECIFY_STATE: strict
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-66974

// FILE: FromJava.java

import org.jspecify.annotations.*;

@NullMarked
public class FromJava<T extends Object> {
    public @Nullable T produce() { return null; }
    public static FromJava<?> IMPLICIT_BOUNDS = new FromJava<String>();
    public static FromJava<? extends String> EXPLICIT_UPPER_BOUND = new FromJava<String>();
    public static FromJava<? super String> EXPLICIT_LOWER_BOUND = new FromJava<String>();
}

// FILE: kotlin.kt

fun <T> accept(arg: T) {}

fun test() {
    accept<Any>(<!ARGUMENT_TYPE_MISMATCH!>FromJava.IMPLICIT_BOUNDS.produce()<!>)
    accept<String>(<!ARGUMENT_TYPE_MISMATCH!>FromJava.EXPLICIT_UPPER_BOUND.produce()<!>)
    accept<Any>(<!ARGUMENT_TYPE_MISMATCH!>FromJava.EXPLICIT_LOWER_BOUND.produce()<!>)
}
