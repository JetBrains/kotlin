// ISSUE: KT-47884

interface A<X>
class B<T>

fun foo(a: A<*>, b: B<*>): Boolean = a == b

fun bar(a: A<*>, b: B<*>): Boolean = a === b
