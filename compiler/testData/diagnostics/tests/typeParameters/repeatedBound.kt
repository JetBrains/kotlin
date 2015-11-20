interface I1

class A1<T> where T : I1, T : <!REPEATED_BOUND!>I1<!>
class A2<T> where T : I1, T : <!REPEATED_BOUND!>I1?<!>
class A3<K, V> where K : V, K : <!REPEATED_BOUND!>V<!>

fun <T> f1() where T : I1, T : <!REPEATED_BOUND!>I1<!> {}
