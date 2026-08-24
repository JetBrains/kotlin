// TARGET_BACKEND: JVM
// CHECK_BYTECODE_TEXT
// ISSUE: KT-88455
// FILE: JavaMethodAnn.java

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
public @interface JavaMethodAnn {
}

// FILE: javaAnnotationOnObjectLiteral.kt

fun box(): String {
    val o = @JavaMethodAnn object : Any() {}
    return "OK"
}

// 0 @LJavaMethodAnn;
