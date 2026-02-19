// RUN_PIPELINE_TILL: BACKEND
// FILE: MyRunnable.java
public interface MyRunnable {
    Object foo(int x);
    default void bar() {}
}

// FILE: DerivedRunnable.java
public interface DerivedRunnable extends MyRunnable {
    @Override
    Boolean foo(int x);
    default void baz() {}
}

// FILE: JavaUsage.java

public class JavaUsage {
    public static void foo(DerivedRunnable x) {}
}
// FILE: main.kt

fun foo(m: MyRunnable) {}

fun main() {
    JavaUsage.foo {
            x ->
        x > 1
    }

    JavaUsage.foo({ it > 1 })

    val x = { x: Int -> x > 1 }

    JavaUsage.foo(x)
}

/* GENERATED_FIR_TAGS: comparisonExpression, flexibleType, functionDeclaration, integerLiteral, javaFunction, javaType,
lambdaLiteral, localProperty, propertyDeclaration, samConversion */
