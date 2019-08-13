// FILE: samConversionToGeneric.kt

fun test1() = J<String> { x -> x }

fun test2() = J { x: String -> x }

fun test3() = H.bar { x: String  -> x }

fun test4(a: Any) {
    a as J<String>
    H.bar(a)
}

fun test5(a: Any) {
    a as (String) -> String
    H.bar(a)
}

fun <T> test6(a: (T) -> T) {
    H.bar(a)
}

fun <T> test7(a: Any) {
    a as (T) -> T
    H.bar(a)
}

fun test8(efn: String.() -> String) = J(efn)

fun test9(efn: String.() -> String) {
    H.bar(efn)
}

fun test10(fn: (Int) -> String) {
    H.bar2x(fn)
}

// FILE: J.java
public interface J<T> {
    T foo(T x);
}

// FILE: J2.java
public interface J2<T1, T2> {
    T1 foo(T2 x);
}

// FILE: J2X.java
public interface J2X<T3> extends J2<String, T3> {
}

// FILE: H.java
public class H {
    public static <X> void bar(J<X> j) {}
    public static <Y> void bar2x(J2X<Y> j2x) {}
}