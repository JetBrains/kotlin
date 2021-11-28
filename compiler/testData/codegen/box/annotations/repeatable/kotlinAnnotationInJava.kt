// !LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK

// java.lang.NoSuchMethodError: java.lang.Class.getAnnotationsByType
// IGNORE_BACKEND: ANDROID

// FILE: box.kt

@Repeatable
annotation class A(val value: String)

fun box(): String {
    val annotations = Z::class.java.annotations
    val aa = annotations.singleOrNull() ?: return "Fail 1: $annotations"

    val a = ContainerSupport.load(aa)
    if (a.size != 3) return "Fail 2: $a"

    val bytype = Z::class.java.getAnnotationsByType(A::class.java)
    if (a.toList() != bytype.toList()) return "Fail 3: ${a.toList()} != ${bytype.toList()}"

    return a.fold("") { acc, it -> acc + it.value }
}

// FILE: Z.java

@A("O")
@A("")
@A("K")
public class Z {}

// FILE: ContainerSupport.java

import java.lang.annotation.Annotation;

public class ContainerSupport {
    public static A[] load(Annotation container) {
        return ((A.Container) container).value();
    }
}
