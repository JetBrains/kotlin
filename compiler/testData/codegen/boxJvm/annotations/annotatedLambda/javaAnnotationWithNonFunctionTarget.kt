// TARGET_BACKEND: JVM
// CHECK_BYTECODE_TEXT
// ISSUE: KT-88455
// FILE: JavaTypeAnn.java

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface JavaTypeAnn {
}

// FILE: javaAnnotationWithNonFunctionTarget.kt

fun consume(block: () -> Unit) = block

fun box(): String {
    consume(@JavaTypeAnn { })
    return "OK"
}

// 0 @LJavaTypeAnn;
