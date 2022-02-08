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

@Retention(RetentionPolicy.RUNTIME)
@Documented
@CheckForNull
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface NullableApi {
}

// FILE: FieldsAreNullable.java
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.CheckForNull;
import javax.annotation.meta.TypeQualifierDefault;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@CheckForNull
@TypeQualifierDefault({ElementType.FIELD})
public @interface FieldsAreNullable {
}

// FILE: A.java
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;

@NonNullApi
public class A {
    public String field = null;

    public String foo(String x, @CheckForNull CharSequence y) {
        return "";
    }

    @NullableApi
    public String foobar(String x, @Nonnull CharSequence y) {
        return "";
    }

    public String bar() {
        return "";
    }

    @Nullable
    public java.util.List<String> baz() {
        return null;
    }

    @NullableApi
    public class B {
        public String field = null;

        public String foo(String x, @Nonnull CharSequence y) {
            return "";
        }

        @NonNullApi
        public String foobar(String x, @Nullable CharSequence y) {
            return "";
        }

        public String bar() {
            return "";
        }

        @Nullable
        public java.util.List<String> baz() {
            return null;
        }
    }

    @FieldsAreNullable
    public class C {
        public String field = null;

        public String foo(String x, @Nullable CharSequence y) {
            return "";
        }

        @NullableApi
        public String foobar(String x, @Nullable CharSequence y) {
            return "";
        }

        public String bar() {
            return "";
        }

        @Nullable
        public java.util.List<String> baz() {
            return null;
        }
    }
}

// FILE: main.kt
fun main(a: A, b: A.B, c: A.C) {
    a.foo("", null)?.length
    a.foo("", null).length
    a.foo(null, "").length

    a.foobar(null, "").length
    a.foobar("", <!NULL_FOR_NONNULL_TYPE!>null<!>)?.length

    a.bar().length
    a.bar()!!.length

    a.field?.length
    a.field.length

    a.baz()<!UNSAFE_CALL!>.<!>get(0)
    a.baz()!!.get(0).get(0)
    a.baz()!!.get(0)?.get(0)

    // b
    b.foo("", <!NULL_FOR_NONNULL_TYPE!>null<!>)?.length
    b.foo("", <!NULL_FOR_NONNULL_TYPE!>null<!>).length
    b.foo(null, "").length

    b.foobar(null, "").length
    b.foobar("", null)?.length

    b.bar().length
    b.bar()!!.length

    b.field?.length
    b.field.length

    b.baz()<!UNSAFE_CALL!>.<!>get(0)
    b.baz()!!.get(0).get(0)
    b.baz()!!.get(0)?.get(0)

    // c
    c.foo("", null)?.length
    c.foo("", null).length
    c.foo(null, "").length

    c.foobar(null, "").length
    c.foobar("", null)?.length

    c.bar().length
    c.bar()!!.length

    c.field?.length
    c.field.length

    c.baz()<!UNSAFE_CALL!>.<!>get(0)
    c.baz()!!.get(0).get(0)
    c.baz()!!.get(0)?.get(0)
}
