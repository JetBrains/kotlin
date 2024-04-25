// FIR_IDENTICAL
// LANGUAGE: -RepeatableAnnotations
// FULL_JDK
// FILE: Runtime.java

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Runtime {}

// FILE: Clazz.java

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
public @interface Clazz {}

// FILE: Source.java

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
public @interface Source {}

// FILE: usage.kt

@Runtime <!REPEATED_ANNOTATION!>@Runtime<!>
class UseRuntime

@Clazz <!REPEATED_ANNOTATION!>@Clazz<!>
class UseClazz

@Source <!REPEATED_ANNOTATION!>@Source<!>
class UseSource
