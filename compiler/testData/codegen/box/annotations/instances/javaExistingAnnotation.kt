// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_DEXING
// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

// MODULE: lib
// FILE: LibAnnotation.java

package a;

public @interface LibAnnotation {
    long longValue() default 4;
    String stringValue() default "OK";
    Class<?> classValue() default String.class;
    float[] floatArrayValue() default { 9.0f, 10.0f };
}

// MODULE: app(lib)
// FILE: app.kt

package test

import a.*

fun box(): String {
    val l = LibAnnotation()
    if (l.toString() != "@a.LibAnnotation(longValue=4, stringValue=OK, classValue=class java.lang.String, floatArrayValue=[9.0, 10.0])")
        return l.toString()
    return "OK"
}
