// !LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8

// java.lang.NoSuchMethodError: java.lang.Class.getAnnotationsByType
// IGNORE_BACKEND: ANDROID

// In light analysis mode, repeated annotations are not wrapped into the container. This is by design, so that in kapt stubs repeated
// annotations will be visible unwrapped.
// IGNORE_LIGHT_ANALYSIS

// FILE: box.kt

@Repeatable
annotation class A(val value: String)

@A("O")
@A("")
@A("K")
class Z

fun box(): String {
    val annotations = Z::class.java.annotations.filter { it.annotationClass != Metadata::class }
    val aa = annotations.singleOrNull() ?: return "Fail 1: $annotations"

    val a = ContainerSupport.load(aa)
    if (a.size != 3) return "Fail 2: $a"

    val bytype = Z::class.java.getAnnotationsByType(A::class.java)
    if (a.toList() != bytype.toList()) return "Fail 3: ${a.toList()} != ${bytype.toList()}"

    return a.fold("") { acc, it -> acc + it.value }
}

// FILE: ContainerSupport.java

import java.lang.annotation.Annotation;

public class ContainerSupport {
    public static A[] load(Annotation container) {
        return ((A.Container) container).value();
    }
}
