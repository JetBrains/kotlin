// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: warn

// FILE: NonNullApi.java
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.meta.TypeQualifierDefault;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@MyNonnull
@TypeQualifierDefault({ ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
public @interface NonNullApi {
}

// FILE: MigrationNonNullApi.java
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.meta.TypeQualifierDefault;

import kotlin.annotations.jvm.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@MyNonnull
@UnderMigration(status = MigrationStatus.STRICT)
@TypeQualifierDefault({ ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
public @interface MigrationNonNullApi {
}

// FILE: PolicyFromDefault.java
@NonNullApi
public class PolicyFromDefault {
    public String foo(String x) {
        return x;
    }

    public String bar = "bar";
}

// FILE: PolicyFromNickname.java
@MigrationNonNullApi
public class PolicyFromNickname {
    public String foo(String x) {
        return x;
    }

    public String bar = "bar";
}

// FILE: main.kt
fun main(default: PolicyFromDefault, nickname: PolicyFromNickname) {
    default.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>).length
    default.foo("hello").length

    nickname.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>).length
    nickname.foo("hello").length
}
