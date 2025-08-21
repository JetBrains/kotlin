// RUN_PIPELINE_TILL: FRONTEND
// FILE: MyFunction.java
public interface MyFunction<T, R> {
    R foo(T x);
}

// FILE: main.kt

fun main() {
    MyFunction<Int, String>{ x ->
        x.toInt().toString()
    }

    MyFunction { x: Int ->
        x.toString()
    }

    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>MyFunction<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        ""
    }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaType, lambdaLiteral, stringLiteral */
