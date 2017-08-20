// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

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
    a.foo("", null)<!UNNECESSARY_SAFE_CALL!>?.<!>length
    a.foo("", null).length
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>, "").length

    a.foobar(null, "")<!UNSAFE_CALL!>.<!>length
    a.foobar("", <!NULL_FOR_NONNULL_TYPE!>null<!>)?.length

    a.bar().length
    a.bar()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length

    a.field<!UNNECESSARY_SAFE_CALL!>?.<!>length
    a.field.length

    a.baz()<!UNSAFE_CALL!>.<!>get(0)
    a.baz()!!.get(0).get(0)
    a.baz()!!.get(0)?.get(0)

    // b
    b.foo("", <!NULL_FOR_NONNULL_TYPE!>null<!>)?.length
    b.foo("", <!NULL_FOR_NONNULL_TYPE!>null<!>)<!UNSAFE_CALL!>.<!>length
    b.foo(null, "")<!UNSAFE_CALL!>.<!>length

    b.foobar(<!NULL_FOR_NONNULL_TYPE!>null<!>, "").length
    b.foobar("", null)<!UNNECESSARY_SAFE_CALL!>?.<!>length

    b.bar()<!UNSAFE_CALL!>.<!>length
    b.bar()!!.length

    b.field?.length
    b.field<!UNSAFE_CALL!>.<!>length

    b.baz()<!UNSAFE_CALL!>.<!>get(0)
    b.baz()!!.get(0).get(0)
    b.baz()!!.get(0)?.get(0)

    // c
    c.foo("", null)<!UNNECESSARY_SAFE_CALL!>?.<!>length
    c.foo("", null).length
    c.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>, "").length

    c.foobar(null, "")<!UNSAFE_CALL!>.<!>length
    c.foobar("", null)?.length

    c.bar().length
    c.bar()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length

    c.field?.length
    c.field<!UNSAFE_CALL!>.<!>length

    c.baz()<!UNSAFE_CALL!>.<!>get(0)
    c.baz()!!.get(0).get(0)
    c.baz()!!.get(0)?.get(0)
}
