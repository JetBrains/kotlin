// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: warn

// FILE: test/NonNullApi.java
package test;

import java.lang.annotation.*;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER, ElementType.PACKAGE})
public @interface NonNullApi {
}

// FILE: test/package-info.java
@NonNullApi
package test;

// FILE: test/Provider.java
package test;

import javax.annotation.*;

public interface Provider<T> {
    T get();
    @Nullable T getNullable();
    void set(T x);
    void setNullable(@Nullable T x);

    T getSet(T x);
    @Nullable T getSetNullable(@Nullable T x);
}

// FILE: test/I.java
package test;

import javax.annotation.*;

public interface I<T> {
    T get();
    void set(T x);
}

// FILE: main.kt
package test;

abstract class Multiple<T> : Provider<T>, I<T> {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun get(): T = null!!
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun set(x: T) {} // Missing warning in K1, K2 get's this right
}

abstract class A<T> : Provider<T> {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun get(): T = null!!
    override fun getNullable(): T = null!!
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun set(x: T) {} // Missing warning in K1, K2 get's this right
    <!NOTHING_TO_OVERRIDE!>override<!> fun setNullable(x: T) {}

    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun getSet(x: T): T = x
    <!NOTHING_TO_OVERRIDE!>override<!> fun getSetNullable(x: T): T = x
}

abstract class B<T> : Provider<T> {
    override fun get(): T & Any = null!!
    override fun getNullable(): T? = null!!
    override fun set(x: T & Any) {} // False positive in K1
    override fun setNullable(x: T?) {}

    override fun getSet(x: T & Any): T & Any = x // False positive in K1
    override fun getSetNullable(x: T?): T? = x
}

abstract class C<T> : Provider<T> {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun getSet(x: T): T & Any = x!! // Missing warning in K1, K2 get's this right
    <!NOTHING_TO_OVERRIDE!>override<!> fun getSetNullable(x: T): T? = x
}

abstract class D<T> : Provider<T> {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun getSet(x: T & Any): T = x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!> // False positive in K1
    override fun getSetNullable(x: T?): T = x!!
}