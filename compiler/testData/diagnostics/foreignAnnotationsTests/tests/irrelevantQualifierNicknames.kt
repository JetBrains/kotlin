// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

// FILE: UnknownQualifier.java
import javax.annotation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifier;
import javax.annotation.meta.When;

@Documented
@TypeQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface UnknownQualifier {
}

// FILE: UnknownQualifierNickname.java
import javax.annotation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

@Documented
@TypeQualifierNickname
@UnknownQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface UnknownQualifierNickname {
}

// FILE: UnknownQualifierDefault.java
import javax.annotation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.When;

@Documented
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER})
@UnknownQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface UnknownQualifierDefault {
}

// FILE: UnknownQualifierNicknameDefault.java
import javax.annotation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.When;

@Documented
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER})
@UnknownQualifierNickname
@Retention(RetentionPolicy.RUNTIME)
public @interface UnknownQualifierNicknameDefault {
}

// FILE: A.java
import javax.annotation.*;

@UnknownQualifierDefault
public class A {
    @UnknownQualifierNicknameDefault
    public static class B {
        @UnknownQualifier
        public static String foo(@UnknownQualifierNickname String x) { return null; }
    }
}

// FILE: main.kt
fun main() {
    A.B.foo(null).hashCode()
}
