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

    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>MyFunction<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> ->
        ""
    }
}
