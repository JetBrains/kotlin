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
val test3 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>CStr<!> { <!CANNOT_INFER_PARAMETER_TYPE!>s<!> -> <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(s.<!UNRESOLVED_REFERENCE!>length<!>) }
val test4 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>CStrList<!> { <!CANNOT_INFER_PARAMETER_TYPE!>ss<!> -> for (s in <!ITERATOR_AMBIGUITY!>ss<!>) { <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(s.<!UNRESOLVED_REFERENCE!>length<!>) } }
val test5 = <!INAPPLICABLE_CANDIDATE!>C2<!><<!CANNOT_INFER_PARAMETER_TYPE!>Int<!>> { <!CANNOT_INFER_PARAMETER_TYPE!>a<!>, <!CANNOT_INFER_PARAMETER_TYPE!>b<!> -> val x: Int = a <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> b; println(x)}
