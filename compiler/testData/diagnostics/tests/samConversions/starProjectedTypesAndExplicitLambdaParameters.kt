// FULL_JDK
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-29164
// FILE: OneParamLambda.java

public interface OneParamLambda<R, P> {
    R execute(P param);
}

// FILE: TwoParamLambda.java

public interface TwoParamLambda<R, P1, P2> {
    R execute(P1 param1, P2 param2);
}

// FILE: A.java

public class A {
    public static void foo(OneParamLambda<?, ?> handler) { }
    public static void foo(TwoParamLambda<?, ?, ?> handler) { }
}

// FILE: Test.kt

fun bar1(x: java.util.function.Function<*, *>) {} // SAM
fun bar2(x: Function1<*, *>) {} // Regular function type with star projections

fun test() {
    A.<!NONE_APPLICABLE!>foo<!> { i: Int -> i } // A.foo((Integer i) -> ""); // works in Java
    bar1 <!TYPE_MISMATCH!>{ <!EXPECTED_PARAMETER_TYPE_MISMATCH!>i: Int<!> -> i }<!>
    bar2 { <!EXPECTED_PARAMETER_TYPE_MISMATCH!>i: Int<!> -> i }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, lambdaLiteral, nullableType, samConversion, starProjection */
