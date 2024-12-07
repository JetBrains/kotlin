// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT: strict

// FILE: MyNullable.java
import javax.annotation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

@Documented
@TypeQualifierNickname
@Nonnull(when = When.NEVER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyNullable {

}

// FILE: A.java
import javax.annotation.*;
import javax.annotation.meta.When;

public class A {
    @Nonnull(when = When.NEVER) public String field = null;

    @MyNullable
    public String foo(@Nonnull(when = When.NEVER) String x, @MyNullable CharSequence y) {
        return "";
    }
}
// FILE: main.kt
fun main(a: A) {
    a.foo("", null)?.length
    a.foo("", null)<!UNSAFE_CALL!>.<!>length
    a.foo(null, "")<!UNSAFE_CALL!>.<!>length

    a.field?.length
    a.field<!UNSAFE_CALL!>.<!>length
}
