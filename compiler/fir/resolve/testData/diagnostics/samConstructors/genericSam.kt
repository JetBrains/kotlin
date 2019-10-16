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

    MyFunction { x ->
        ""
    }
}
