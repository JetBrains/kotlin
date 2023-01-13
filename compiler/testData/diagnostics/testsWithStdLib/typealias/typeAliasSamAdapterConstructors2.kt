// FIR_IDENTICAL
// FILE: JHost.java
public class JHost {
    public static interface Runnable {
        void run();
    }

    public static interface Consumer<T> {
        void consume(T x);
    }

    public static interface Consumer2<T1, T2> {
        void run(T1 x1, T2 x2);
    }
}

// FILE: test.kt
typealias R = JHost.Runnable
typealias C<T> = JHost.Consumer<T>
typealias CStr = JHost.Consumer<String>
typealias CStrList = JHost.Consumer<List<String>>
typealias C2<T> = JHost.Consumer2<T, T>

val test1 = R { }
val test2 = C<String> { s -> println(s.length) }
val test3 = CStr { s -> println(s.length) }
val test4 = CStrList { ss -> for (s in ss) { println(s.length) } }
val test5 = C2<Int> { a, b -> val x: Int = a + b; println(x)}
