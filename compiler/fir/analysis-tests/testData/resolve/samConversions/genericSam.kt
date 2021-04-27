// FILE: MyFunction.java
public interface MyFunction<T, R> {
    R foo(T x);
}

// FILE: JavaUsage.java

public class JavaUsage {
    public static void foo1(MyFunction<Integer, String> x) {}
    public static void foo2(MyFunction<? super Number, ? extends CharSequence> x) {}
    public static <X, Y> Y foo3(MyFunction<X, Y> f, X x) {}
}
// FILE: main.kt

fun main() {
    JavaUsage.foo1 { x ->
        x.toInt().toString()
    }

    JavaUsage.foo2 { x ->
        x.toInt().toString()
    }

    JavaUsage.foo2 <!ARGUMENT_TYPE_MISMATCH!>{ x: Int ->
        x.toString()
    }<!>

    JavaUsage.foo3(
        { x ->
            (x + 1).toString()
        },
        1
    )

    JavaUsage.foo3(
        { x: Number ->
            x.toInt().toString()
        },
        2
    )
}
