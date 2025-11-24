// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// FULL_JDK
// WITH_STDLIB
// ISSUE: KT-7052

// FILE: MyComputable.java
public interface MyComputable<V> {
    V compute();
}

// FILE: MyThrowableComputable.java
public interface MyThrowableComputable<T, E extends Throwable> {
    T compute() throws E;
}

// FILE: MyJavaClass.java
public class MyJavaClass {
    public static void runSomething(Runnable computable) {
    }

    public static <T> T runSomething(MyComputable<T> computable) {
        return null;
    }

    public static <T, E extends Throwable> T runSomething(MyThrowableComputable<T, E> computable) throws E {
        return null;
    }

    public static <T> T runSomething2(MyComputable<T> computable) {
        return null;
    }

    public static <T, E extends Throwable> T runSomething2(MyThrowableComputable<T, E> computable) throws E {
        return null;
    }
}

// FILE: main.kt
fun main() {
    MyJavaClass.runSomething { "" }
    MyJavaClass.<!OVERLOAD_RESOLUTION_AMBIGUITY!>runSomething2<!> { "" }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, stringLiteral */
