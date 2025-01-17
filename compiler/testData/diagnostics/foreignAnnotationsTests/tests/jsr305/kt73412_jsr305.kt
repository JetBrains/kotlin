// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: strict
// KT-73412, works with jspecify, see kt73412_jspecify.kt

// FILE: ann/Nullable.java
package ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull(when = When.MAYBE)
@TypeQualifierNickname
public @interface Nullable {
}

// FILE: ann/NonNullApi.java
package ann;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER})
public @interface NonNullApi {
}

// FILE: api/package-info.java
@ann.NonNullApi
package api;

// FILE: api/Transformer.java
package api;

public interface Transformer<OUT extends @ann.Nullable Object, IN> {
    OUT transform(IN in);
}

// FILE: api/Consumer.java
package api;

public class Consumer {
    public Consumer applyNullable(Transformer<@ann.Nullable String, String> transformer) { return this; }
    public Consumer applyNotNull(Transformer<String, String> transformer) { return this; }
}

// FILE: main.kt

import api.*

fun main() {
    Consumer().applyNullable { null } // should accept null
    Consumer().applyNotNull { <!NULL_FOR_NONNULL_TYPE, NULL_FOR_NONNULL_TYPE!>null<!> } // expected nullness error
}
