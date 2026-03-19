// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-47544

// FILE: ReduceFunction.java

public interface ReduceFunction<T> {
    T reduce(T v1, T v2);
}

// FILE: Function2.java

public interface Function2<A, B, C> {
    C apply(A a, B b);
}

// FILE: JavaClass.java

public class JavaClass {
    public void javaFunction(ReduceFunction<Integer> func) {}

    @kotlin.Deprecated(message = "", level = kotlin.DeprecationLevel.HIDDEN)
    public void javaFunction(Function2<Integer, Integer, Integer> func) {}
}

// FILE: kt47544.kt

// KT-47544: @kotlin.Deprecated(level = HIDDEN) on Java method should hide it from overload resolution

fun test(c: JavaClass) {
    // The HIDDEN overload javaFunction(Function2) should be invisible,
    // so this should resolve unambiguously to javaFunction(ReduceFunction<Integer>)
    c.javaFunction { a, b -> a + b }
}

/* GENERATED_FIR_TAGS: additiveExpression, flexibleType, functionDeclaration, javaFunction, javaType, lambdaLiteral,
samConversion */
