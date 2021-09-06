// FIR_IDENTICAL
// FILE: lib/NonNullApi.java

package lib;

import java.lang.annotation.*;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
@Nonnull
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER})
public @interface NonNullApi {}

// FILE: lib/package-info.java

@NonNullApi
package lib;

// FILE: lib/A.java

package lib;

public @interface A {
    Class value() default String.class;
}

// FILE: test.kt

import lib.A

@A
fun test() {}
