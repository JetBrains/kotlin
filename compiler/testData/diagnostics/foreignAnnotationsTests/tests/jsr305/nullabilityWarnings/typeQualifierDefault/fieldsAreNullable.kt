// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: warn

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

// FILE: main.kt
fun main(a: A) {
    // foo is platform
    a.foo("", "", null)?.length
    a.foo("", "", null).length
    a.foo(null, <!NULL_FOR_NONNULL_TYPE!>null<!>, "").length

    a.bar().length
    a.bar()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length

    a.field?.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.field<!>.length
    a.field = null

    a.nonNullField<!UNNECESSARY_SAFE_CALL!>?.<!>length
    a.nonNullField.length
    a.nonNullField = <!NULL_FOR_NONNULL_TYPE!>null<!>
}
