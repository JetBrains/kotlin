// !LANGUAGE: +NewInference
// !CHECK_TYPE
// FILE: Fn.java
public interface Fn<T, R> {
    R apply(T t);
}

// FILE: Fn2.java
public interface Fn2<T, R> extends Fn<T, R> {}

// FILE: J.java
public interface J {
    String foo(Fn<String, Object> f, Object o);
    int foo(Fn<Object, Object> f, String s); // (Any) -> Any <: (String) -> Any <=> String <: Any

    String bas(Fn<Object, Object> f, Object o);
    int bas(Fn<Object, String> f, String s); // (Any) -> String <: (Any) -> Any <=> String <: Any

    String bar(Fn<String, Object> f);
    int bar(Fn2<String, Object> f); // Fn2 seems more specific one even function type same
}

// FILE: 1.kt
fun test(j: J) {
    j.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>({ <!UNRESOLVED_REFERENCE!>it<!> checkType { _<Any>() }; "" }, "") <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<Int>() }

    j.<!OVERLOAD_RESOLUTION_AMBIGUITY!>bas<!>({ <!UNRESOLVED_REFERENCE!>it<!> checkType { _<Any>() }; "" }, "") <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<Int>() }

    // NI: TODO
    j.bar { it checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Any>() }; "" } checkType { _<Int>() }
}
