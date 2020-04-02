// FILE: test.kt
typealias RunnableT = java.lang.Runnable
typealias ComparatorT<T> = java.util.Comparator<T>
typealias ComparatorStrT = ComparatorT<String>

val test1 = RunnableT { }
val test2 = ComparatorT<String> { s1, s2 -> s1.compareTo(s2) }
val test3 = ComparatorStrT { s1, s2 -> s1.<!INAPPLICABLE_CANDIDATE!>compareTo<!>(s2) }
