// FOREIGN_ANNOTATIONS

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

import javax.annotation.*;

@FieldsAreNullable
public class A {
    public String field = null;
    @Nonnull
    public String nonNullField = "";

    public String foo(String q, @Nonnull String x, @CheckForNull CharSequence y) {
        return "";
    }

    @Nonnull
    public String bar() {
        return "";
    }
}

