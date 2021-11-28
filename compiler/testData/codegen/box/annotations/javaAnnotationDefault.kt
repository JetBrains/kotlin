// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// FILE: JavaAnn.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    String value() default "default";
}

// FILE: JavaAnn2.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn2 {
    int a() default 1;
    byte b() default 1;
    short c() default 1;
    double d() default 1;
    float e() default 1;
    long j() default 1;
    String f() default "default";
    Class<?> g() default JavaAnn2.class;
}

// MODULE: main(lib)
// FILE: 1.kt

@JavaAnn class MyClass
@JavaAnn2 class MyClass2

fun box(): String {
    val ann = MyClass::class.java.getAnnotation(JavaAnn::class.java)
    if (ann == null) return "fail: cannot find Ann on MyClass}"
    if (ann.value != "default") return "fail: annotation parameter i should be 'default', but was ${ann.value}"

    val ann2 = MyClass2::class.java.getAnnotation(JavaAnn2::class.java)
    if (ann2 == null) return "fail: cannot find Ann on MyClass}"
    if (ann2.a != 1) return "fail for a: expected = 1, but was ${ann2.a}"
    if (ann2.b != 1.toByte()) return "fail for b: expected = 1, but was ${ann2.b}"
    if (ann2.c != 1.toShort()) return "fail for c: expected = 1, but was ${ann2.c}"
    if (ann2.d != 1.0) return "fail for d: expected = 1, but was ${ann2.d}"
    if (ann2.e != 1F) return "fail for e: expected = 1, but was ${ann2.e}"
    if (ann2.j != 1L) return "fail for j: expected = 1, but was ${ann2.j}"
    if (ann2.f != "default") return "fail for f: expected = default, but was ${ann2.f}"
    if (ann2.g != JavaAnn2::class) return "fail for g: expected = JavaAnn2, but was ${ann2.g}"

    return "OK"
}
