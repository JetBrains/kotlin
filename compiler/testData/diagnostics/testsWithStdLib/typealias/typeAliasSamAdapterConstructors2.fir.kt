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

val test1 = <!UNRESOLVED_REFERENCE!>R<!> { }
val test2 = <!UNRESOLVED_REFERENCE!>C<!><String> { s -> <!AMBIGUITY!>println<!>(s.<!UNRESOLVED_REFERENCE!>length<!>) }
val test3 = <!UNRESOLVED_REFERENCE!>CStr<!> { s -> <!AMBIGUITY!>println<!>(s.<!UNRESOLVED_REFERENCE!>length<!>) }
val test4 = <!UNRESOLVED_REFERENCE!>CStrList<!> { ss -> <!AMBIGUITY, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for (s in ss) { <!AMBIGUITY!>println<!>(s.<!UNRESOLVED_REFERENCE!>length<!>) }<!> }
val test5 = <!UNRESOLVED_REFERENCE!>C2<!><Int> { a, b -> val x: Int = a <!AMBIGUITY!>+<!> b; println(x)}
