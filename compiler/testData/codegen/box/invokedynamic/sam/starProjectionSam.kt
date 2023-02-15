// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: box.kt

fun box(): String {
    return Request.foo { x, y -> "OK" }
}

// FILE: Request.java
public class Request {
    public static String foo(Func<?, ?, ?> x) {
        return ((Func) x).bar(null, null).toString();
    }
}

// FILE: Func.java

import java.util.List;

public interface Func<T1 extends List<?>, T2 extends CharSequence, R> {
    R bar(T1 t1, T2 t2);
}
