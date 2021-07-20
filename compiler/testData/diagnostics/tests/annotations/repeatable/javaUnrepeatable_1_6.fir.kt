// !LANGUAGE: +RepeatableAnnotations
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

@Runtime @Runtime
class UseRuntime

@Clazz @Clazz
class UseClazz

@Source @Source
class UseSource
