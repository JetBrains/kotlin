// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: strict
// ISSUE: KT-55548
// FULL_JDK

// FILE: spr/NonNullApi.java
package spr;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Target({ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER})
public @interface NonNullApi {
}

// FILE: test/package-info.java
@spr.NonNullApi()
package test;

// FILE: test/CrudRepository.java
package test;
import spr.*;

public interface CrudRepository<T, ID> {
    java.util.Optional<T> findById(ID id);
}

// FILE: main.kt
import test.CrudRepository
import java.util.*

fun test(repository: SpringTemplateRepository, int: Int) {
    repository.findById(int)
}

interface SpringTemplateRepository : TemplateRepository, CrudRepository<String, Int>

interface TemplateRepository {
    fun findById(id: Int): Optional<String>
}
