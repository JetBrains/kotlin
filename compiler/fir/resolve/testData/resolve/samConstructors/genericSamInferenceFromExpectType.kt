// FILE: MyFunction.java
public interface MyFunction<T, R> {
    R foo(T x);
}

// FILE: main.kt

fun foo1(x: MyFunction<Int, String>) {}
fun foo2(x: MyFunction<in Number, out CharSequence>) {}
fun <X, Y> foo3(f: MyFunction<X, Y>, x: X) {}

fun main() {
    foo1(MyFunction { x ->
        x.toInt().toString()
    })

    foo2(MyFunction { x ->
        x.toInt().toString()
    })

    <!INAPPLICABLE_CANDIDATE!>foo2<!>(MyFunction { x: Int ->
        x.toString()
    })

    foo3(
       MyFunction { x ->
            (x + 1).toString()
        },
        1
    )

    foo3(
        MyFunction { x: Number ->
            x.toInt().toString()
        },
        2
    )
}
