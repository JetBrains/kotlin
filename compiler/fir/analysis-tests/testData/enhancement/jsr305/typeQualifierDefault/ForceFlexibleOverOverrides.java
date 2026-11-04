// FOREIGN_ANNOTATIONS

// FILE: spr/NonNullApi.java

package spr;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER})
public @interface NonNullApi {
}

// FILE: spr/UnknownNullability.java
package spr;

import javax.annotation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

@Documented
@TypeQualifierNickname
@Nonnull(when = When.UNKNOWN)
@Retention(RetentionPolicy.RUNTIME)
public @interface UnknownNullability {

}

// FILE: spr/ForceFlexibility.java

package spr;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.When;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@UnknownNullability
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER})
public @interface ForceFlexibility {
}

// FILE: B.java
public interface B {
    public void foo(@javax.annotation.Nonnull String x);
    public void bar(@javax.annotation.Nonnull String x);
    public void baz(@javax.annotation.Nonnull String x);
    public void foobar(@javax.annotation.Nonnull String x);
}

// FILE: A.java

import spr.*;

@NonNullApi
public class A implements B {
    @ForceFlexibility
    public void foo(String x) {}
    public void bar(@ForceFlexibility String x) {}
    public void baz(@UnknownNullability String x) {}
    public void foobar(@javax.annotation.Nonnull(when = javax.annotation.meta.When.UNKNOWN) String x) {}
}

