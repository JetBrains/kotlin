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

    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>MyFunction<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        ""
    }
}
