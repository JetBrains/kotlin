// FOREIGN_ANNOTATIONS

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
    public String foobar(String x, @NonNullApi CharSequence y) {
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
        public String foobar(String x, @NullableApi CharSequence y) {
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

