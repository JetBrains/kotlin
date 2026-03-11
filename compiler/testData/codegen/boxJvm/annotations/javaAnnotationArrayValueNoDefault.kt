// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// FILE: JavaAnn.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    String[] value();
}

// MODULE: main(lib)
// FILE: 1.kt

@JavaAnn class MyClass1
@JavaAnn() class MyClass2
@JavaAnn("asd") class MyClass3
@JavaAnn(*arrayOf()) class MyClass4


fun box(): String {
    val value1 = MyClass1::class.java.getAnnotation(JavaAnn::class.java).value
    if (value1.size != 0) return "fail1: ${value1.size}"

    val value2 = MyClass2::class.java.getAnnotation(JavaAnn::class.java).value
    if (value2.size != 0) return "fail2: ${value2.size}"

    val value3 = MyClass3::class.java.getAnnotation(JavaAnn::class.java).value
    if (value3.size != 1) return "fail3: ${value3.size}"
    if (value3[0] != "asd") return "fail4: ${value3[0]}"

    val value4 = MyClass4::class.java.getAnnotation(JavaAnn::class.java).value
    if (value4.size != 0) return "fail 5: ${value4.size}"

    return "OK"
}
