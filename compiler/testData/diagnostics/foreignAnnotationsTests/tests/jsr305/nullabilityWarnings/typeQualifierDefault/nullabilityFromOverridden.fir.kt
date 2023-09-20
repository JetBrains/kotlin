// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: warn

// FILE: NonNullApi.java
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface NonNullApi {
}

// FILE: NullableApi.java
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.CheckForNull;
import javax.annotation.meta.TypeQualifierDefault;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@CheckForNull
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface NullableApi {
}

// FILE: A.java
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;

@NonNullApi
public class A {
    public String foo1(String x) { return ""; }
    public String foo2(String x) { return ""; }
    public String foo3(String x) { return ""; }


    @Nullable
    public String bar1(@Nullable String x) { return ""; }
    @Nullable
    public String bar2(@Nullable String x) { return ""; }

    public String baz(@Nonnull String x) { return ""; }
}

// FILE: AInt.java
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;

@NonNullApi
public interface AInt {
    public CharSequence foo1(String x);
    public CharSequence foo2(String x);
    public CharSequence foo3(String x);


    @Nullable
    public CharSequence bar1(@Nullable String x);
    @Nullable
    public CharSequence bar2(@Nullable String x);

    public CharSequence baz(@Nonnull String x);
}


// FILE: B.java
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;

@NullableApi
public class B extends A implements AInt {
    // conflicts
    public String foo1(String x) { return ""; }

    // no conflicts
    @Nonnull
    public String foo2(@Nonnull String x) { return ""; }

    // fake override for foo3 shouldn't have any conflicts

    // no conflicts
    public String bar1(String x) { return ""; }

    // conflicts
    public String baz(String x) { return ""; }
}

// FILE: C.java
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;

@NonNullApi
public class C extends A implements AInt {
    // no conflicts
    public String foo1(String x) { return ""; }

    // no conflicts
    public String foo2(@Nonnull String x) { return ""; }

    // fake override for foo3 shouldn't have any conflicts

    // no conflicts, covariant override
    public String bar1(String x) { return ""; }
    // no conflicts
    @Nullable
    public String bar2(@Nullable String x) { return ""; }

    // no conflicts
    public String baz(String x) { return ""; }
}

// FILE: main.kt
fun main(a: A, b: B, c: C) {
    a.foo1(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>).length
    a.foo2(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>).length
    a.foo3(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>).length
    a.bar1(null)<!UNSAFE_CALL!>.<!>length
    a.bar2(null)<!UNSAFE_CALL!>.<!>length
    a.baz(<!NULL_FOR_NONNULL_TYPE!>null<!>).length

    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>b.foo1(null)<!>.length
    b.foo1(null)?.length
    b.foo2(<!NULL_FOR_NONNULL_TYPE!>null<!>).length
    b.foo3(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>).length
    b.bar1(null)<!UNSAFE_CALL!>.<!>length
    b.bar2(null)<!UNSAFE_CALL!>.<!>length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>b.baz(<!NULL_FOR_NONNULL_TYPE!>null<!>)<!>.length
    b.baz(<!NULL_FOR_NONNULL_TYPE!>null<!>)?.length

    c.foo1(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>).length
    c.foo2(<!NULL_FOR_NONNULL_TYPE!>null<!>).length
    c.foo3(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>).length
    c.bar1(null)<!UNSAFE_CALL!>.<!>length
    c.bar1(null)?.length
    c.bar2(null)<!UNSAFE_CALL!>.<!>length
    c.baz(<!NULL_FOR_NONNULL_TYPE!>null<!>).length
}
