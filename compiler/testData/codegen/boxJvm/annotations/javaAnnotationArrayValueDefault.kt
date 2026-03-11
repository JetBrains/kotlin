// TARGET_BACKEND: JVM
// WITH_STDLIB
// DUMP_IR
// MODULE: lib
// FILE: JavaAnn.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    String[] value() default {"d1", "d2"};
}

// MODULE: main(lib)
// FILE: 1.kt

@JavaAnn class MyClass1
@JavaAnn() class MyClass2
@JavaAnn("asd") class MyClass3
@JavaAnn(*arrayOf()) class MyClass4


fun box(): String {
    val value1 = MyClass1::class.java.getAnnotation(JavaAnn::class.java).value
    if (value1.size != 2) return "fail1: ${value1.size}"
    if (value1[0] != "d1") return "fail2: ${value1[0]}"
    if (value1[1] != "d2") return "fail3: ${value1[1]}"

    val value2 = MyClass2::class.java.getAnnotation(JavaAnn::class.java).value
    if (value2.size != 2) return "fail4: ${value2.size}"
    if (value2[0] != "d1") return "fail5: ${value2[0]}"
    if (value2[1] != "d2") return "fail6: ${value2[1]}"

    val value3 = MyClass3::class.java.getAnnotation(JavaAnn::class.java).value
    if (value3.size != 1) return "fail7: ${value3.size}"
    if (value3[0] != "asd") return "fail8: ${value3[0]}"

    val value4 = MyClass4::class.java.getAnnotation(JavaAnn::class.java).value
    if (value4.size != 0) return "fail 9: ${value4.size}"

    return "OK"
}
