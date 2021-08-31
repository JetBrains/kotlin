// !LANGUAGE: +NewInference +SamConversionForKotlinFunctions +SamConversionPerArgument
// !CHECK_TYPE
// FILE: Fn.java
public interface Fn<T, R> {
    R apply(T t);
}

// FILE: Fn2.java
public interface Fn2<T, R> extends Fn<T, R> {}

// FILE: 1.kt
interface K {
    fun foo(f: Fn<String, Any>): String
    fun foo(f: Fn<Any, Any>): Int

    fun bas(f: Fn<Any, Any>): String
    fun bas(f: Fn<Any, String>): Int

    fun bar(f: Fn<String, Any>): String
    fun bar(f: Fn2<String, Any>): Int
}

fun test(k: K) {
    k.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> { <!UNRESOLVED_REFERENCE!>it<!> checkType { _<Any>() }; "" } <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<Int>() }

    k.<!OVERLOAD_RESOLUTION_AMBIGUITY!>bas<!> { <!UNRESOLVED_REFERENCE!>it<!> checkType { _<Any?>() }; "" } <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<Int>() }

    // NI: TODO
    k.bar { it checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Any>() }; "" } checkType { _<Int>() }
}
