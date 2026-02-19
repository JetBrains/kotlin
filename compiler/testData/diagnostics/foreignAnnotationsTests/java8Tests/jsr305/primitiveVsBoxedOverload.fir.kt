// SKIP_JAVAC
// JSR305_GLOBAL_REPORT: strict
// LANGUAGE: +OverloadResolutionSpecificityForEnhancedJvmPrimitiveWrappers
// ISSUE: KT-55548

// FILE: spr/NonNullApi.java
package spr;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Target({ElementType.TYPE, ElementType.TYPE_USE, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.TYPE_USE})
public @interface NonNullApi {
}

// FILE: spr/package-info.java
@NonNullApi
package spr;

// FILE: spr/CrudRepository.java
package spr;

public interface CrudRepository<T, ID> {
    java.util.List<T> findById(ID id);
}

// FILE: main.kt
import spr.CrudRepository

interface TemplateRepository {
    fun findById(id: Long): List<String>
}

interface SpringTemplateRepository : TemplateRepository, CrudRepository<String, Long>

fun foo(r: SpringTemplateRepository) {
    r.findById(123).get(0)
}