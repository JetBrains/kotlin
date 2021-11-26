// !LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8

// java.lang.NoSuchMethodError: java.lang.Class.getAnnotationsByType
// IGNORE_BACKEND: ANDROID

// FILE: box.kt

import test.A
import test.As

@A("O")
@A("")
@A("K")
class Z

fun box(): String {
    val annotations = Z::class.java.annotations.filter { it.annotationClass != Metadata::class }
    val aa = annotations.singleOrNull() ?: return "Fail 1: $annotations"
    if (aa !is As) return "Fail 2: $aa"

    val a = aa.value.asList()
    if (a.size != 3) return "Fail 3: $a"

    val bytype = Z::class.java.getAnnotationsByType(A::class.java)
    if (a.toList() != bytype.toList()) return "Fail 4: ${a.toList()} != ${bytype.toList()}"

    return a.fold("") { acc, it -> acc + it.value }
}

// FILE: test/A.java

package test;

import java.lang.annotation.*;

@Repeatable(As.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface A {
    String value();
}

// FILE: test/As.java

package test;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface As {
    A[] value();
}
