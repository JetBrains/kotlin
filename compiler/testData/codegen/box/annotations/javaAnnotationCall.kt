// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// FILE: JavaAnn.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    String value();
}

// MODULE: main(lib)
// FILE: 1.kt

@JavaAnn("value") class MyClass

fun box(): String {
    val ann = MyClass::class.java.getAnnotation(JavaAnn::class.java)
    if (ann == null) return "fail: cannot find Ann on MyClass}"
    if (ann.value != "value") return "fail: annotation parameter i should be 'value', but was ${ann.value}"
    return "OK"
}
