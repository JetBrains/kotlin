fun <<!ELEMENT!>> f1() {}

fun <reified T : <!ELEMENT!>> T.f2() {}

class B<K: L<M<<!ELEMENT!>>>> {}

class B<K, T: A<in <!ELEMENT!>>> {}

fun <T : org.jetbrains.<!ELEMENT!>> T.f3() {}

fun f4(a: List<out <!ELEMENT!>>) {}

fun f5(a: List<List<List<<!ELEMENT!>?>>>) {}
