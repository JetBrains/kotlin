// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// FILE: JavaAnn.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    Class<?> value();
}

// FILE: MyJavaClass.java

class OK {}

@JavaAnn(OK.class)
class MyJavaClass {}

// FILE: 1.kt

fun box(): String {
    val ann = MyJavaClass::class.java.getAnnotation(JavaAnn::class.java)
    if (ann == null) return "fail: cannot find JavaAnn on MyClass"
    return ann.value.java.simpleName!!
}
