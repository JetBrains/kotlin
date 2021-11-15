// FIR_IDENTICAL
// FULL_JDK
// WITH_STDLIB
// WITH_REFLECT
// SKIP_TXT

// FILE: NonNullApi.java

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.PACKAGE})
@javax.annotation.Nonnull
@javax.annotation.meta.TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER})
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface NonNullApi { }

// FILE: Foo.java

import java.util.Collection;

@NonNullApi
public class Foo<E> {
    public Foo(Collection<? extends E> c) {}
}

// FILE: main.kt

fun test() {
    val collection: Collection<Int> = listOf(1, 2, 3)
    Foo(collection)
}
