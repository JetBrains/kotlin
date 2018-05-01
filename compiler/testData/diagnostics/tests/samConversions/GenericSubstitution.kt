// !LANGUAGE: +NewInference
// !CHECK_TYPE
// FILE: J.java
public interface J<T> {
    void f_t(F<T> f1, F<T> f2);
    <R> void f_r(F<R> f1, F<R> f2);
    <R> void f_pr(F<PR<T, R>> f1, F<PR<T, R>> f2);
}

// FILE: F.java
public interface F<S> {
    void apply(S s);
}

// FILE: PR.java
public interface PR<X, Y> {}

// FILE: 1.kt

fun test(
    j: J<String>,
    f_string: F<String>,
    f_int: F<Int>,
    f_pr: F<PR<String, Int>>
) {
    j.f_t(f_string) { it checkType { _<String>() } }
    j.f_r(f_int) { it checkType { _<Int>() } }
    j.f_pr(f_pr) { it checkType { _<PR<String, Int>>() } }
}