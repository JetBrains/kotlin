// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// FILE: JavaAnn.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    Class<?> value();
}

// FILE: 1.kt

class OK

@JavaAnn(OK::class) class MyClass

fun box(): String {
    val ann = MyClass::class.java.getAnnotation(JavaAnn::class.java)
    if (ann == null) return "fail: cannot find JavaAnn on MyClass"
    return ann.value.java.simpleName!!
}
