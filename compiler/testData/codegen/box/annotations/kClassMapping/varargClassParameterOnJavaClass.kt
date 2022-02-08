// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// FILE: JavaAnn.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    Class<?>[] value();
}

class O {}
class K {}

// FILE: MyJavaClass.java

@JavaAnn({O.class, K.class})
class MyJavaClass {}

// MODULE: main(lib)
// FILE: 1.kt

class O
class K

fun box(): String {
    val args = MyJavaClass::class.java.getAnnotation(JavaAnn::class.java).value
    val argName1 = args[0].java.simpleName ?: "fail 1"
    val argName2 = args[1].java.simpleName ?: "fail 2"
    return argName1 + argName2
}
