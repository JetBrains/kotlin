// FILE: test.kt
typealias RunnableT = java.lang.Runnable
typealias ComparatorT<T> = java.util.Comparator<T>
typealias ComparatorStrT = ComparatorT<String>

val test1 = RunnableT { }
val test2 = ComparatorT<String> { s1, s2 -> s1.compareTo(s2) }
val test3 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>ComparatorStrT<!> { <!CANNOT_INFER_PARAMETER_TYPE!>s1<!>, <!CANNOT_INFER_PARAMETER_TYPE!>s2<!> -> s1.compareTo(s2) }
