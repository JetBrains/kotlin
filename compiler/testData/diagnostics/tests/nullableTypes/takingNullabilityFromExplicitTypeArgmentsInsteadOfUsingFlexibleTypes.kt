// !LANGUAGE: +NewInference

// It's relevant only for Java constructor calls

// FILE: J.java

public class J<T extends Integer>  {}

// FILE: main.kt

import java.util.ArrayList

class Foo(val attributes: Map<String, String>)

class A<R>

class Bar<T, K: Any> {
    val foos1 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<Foo>")!>ArrayList<Foo>()<!>
    val foos2 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<Foo?>")!>ArrayList<Foo?>()<!>
    val foos3 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<A<Foo>>")!>ArrayList<A<Foo>>()<!>
    val foos4 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<A<Foo>?>")!>ArrayList<A<Foo>?>()<!>
    val foos5 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<A<Foo?>?>")!>ArrayList<A<Foo?>?>()<!>
    val foos6 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<A<Foo?>>")!>ArrayList<A<Foo?>>()<!>
    val foos7 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<T>")!>ArrayList<T>()<!>
    val foos8 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<T?>")!>ArrayList<T?>()<!>
    val foos9 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<K>")!>ArrayList<K>()<!>
    val foos10 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<K?>")!>ArrayList<K?>()<!>
    val foos11 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<A<K?>>")!>ArrayList<A<K?>>()<!>
    val foos12 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<A<K>>")!>ArrayList<A<K>>()<!>
    val foos13 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<A<T>>")!>ArrayList<A<T>>()<!>
    val foos14 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<A<T>?>")!>ArrayList<A<T>?>()<!>
    val foos15 = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<A<T?>>")!>ArrayList<A<T?>>()<!>

    val foos16 = <!DEBUG_INFO_EXPRESSION_TYPE("J<Foo>")!>J<<!UPPER_BOUND_VIOLATED!>Foo<!>>()<!>
    val foos17 = <!DEBUG_INFO_EXPRESSION_TYPE("J<Foo?>")!>J<<!UPPER_BOUND_VIOLATED!>Foo?<!>>()<!>
    val foos18 = <!DEBUG_INFO_EXPRESSION_TYPE("J<T>")!>J<<!UPPER_BOUND_VIOLATED!>T<!>>()<!>
    val foos19 = <!DEBUG_INFO_EXPRESSION_TYPE("J<T?>")!>J<<!UPPER_BOUND_VIOLATED!>T?<!>>()<!>
}
