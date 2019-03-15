// IGNORE_BACKEND: JVM_IR
// FILE: JavaAnn.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    Class<?>[] args();
}

// FILE: MyJavaClass.java

class O {}
class K {}

@JavaAnn(args = {O.class, K.class})
class MyJavaClass {}

// FILE: 1.kt

fun box(): String {
    val args = MyJavaClass::class.java.getAnnotation(JavaAnn::class.java).args
    val argName1 = args[0].java.simpleName ?: "fail 1"
    val argName2 = args[1].java.simpleName ?: "fail 2"
    return argName1 + argName2
}
