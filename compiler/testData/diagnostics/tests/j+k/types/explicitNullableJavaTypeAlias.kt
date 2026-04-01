// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// FIR_DUMP

// FILE: J.java
public interface J<T> {
    int apply(T x);
}

// FILE: Use.java
public class Use {
    public static <T> int run(J<T> j) {
        return 0;
    }
}

// FILE: JavaUtil.java
public class JavaUtil {
    public static <T> T id(T x) {
        return x;
    }
}

// FILE: main.kt
typealias N = String?
typealias S = String

fun aliasInSam() {
    Use.run<N> { it ->
        it.length
    }
}

fun aliasInGenericFunction() {
    val x = JavaUtil.id<N>("abc")
    x.length
}

fun aliasSmartCastAfterCall(x: N) {
    val y = JavaUtil.id<N>(x)
    if (y != null) {
        y.length
    }
}

fun nonNullAliasBaseline() {
    val x = JavaUtil.id<S>("abc")
    x.length
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, samConversion, stringLiteral, typeAliasDeclaration */
