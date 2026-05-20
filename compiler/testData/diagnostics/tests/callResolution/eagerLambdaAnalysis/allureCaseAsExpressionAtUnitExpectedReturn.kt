// RUN_PIPELINE_TILL: BACKEND
// FILE: ThrowableRunnable.java
public interface ThrowableRunnable<T> {
    T run() throws java.lang.Throwable;
}
// FILE: ThrowableRunnableVoid.java
public interface ThrowableRunnableVoid {
    void run() throws java.lang.Throwable;
}
// FILE: Allure.java
public class Allure {
    public static <T> T step(java.lang.String name, ThrowableRunnable<T> runnable) { return null; }
    public static void step(java.lang.String name, ThrowableRunnableVoid runnable) { }
}

// FILE: main.kt

// Usage of Allure Testing Reports Framework
fun foo(): Unit = Allure.step("foo") { "bar" }

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, lambdaLiteral, samConversion, stringLiteral */
