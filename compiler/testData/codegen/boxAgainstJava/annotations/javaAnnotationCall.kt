// WITH_RUNTIME
// FILE: JavaAnn.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    String value();
}

// FILE: 1.kt

@JavaAnn("value") class MyClass

fun box(): String {
    val ann = MyClass::class.java.getAnnotation(JavaAnn::class.java)
    if (ann == null) return "fail: cannot find Ann on MyClass}"
    if (ann.value != "value") return "fail: annotation parameter i should be 'value', but was ${ann.value}"
    return "OK"
}
