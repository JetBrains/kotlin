// !DIAGNOSTICS: -UNUSED_VARIABLE
package test

object ClassMemberMarker

class a<T> {
    fun foo() = ClassMemberMarker
}

class b<T1, T2> {
    fun foo() = ClassMemberMarker
}

fun Int.foo() {}

class Test {
    val <T> List<T>.a: Int get() = size
    val <T> List<T>.b: Int? get() = size

    fun <T> List<T>.testCallable1(): () -> Unit = a<T>::foo
    fun <T> List<T>.testCallable2(): () -> Unit = <!UNRESOLVED_REFERENCE!>b?::foo<!>
    fun <T> List<T>.testCallable3(): () -> Unit = <!UNRESOLVED_REFERENCE!>b<T, Any>::foo<!>
    fun <T> List<T>.testCallable4(): () -> Unit = <!UNRESOLVED_REFERENCE!>b<T>?::foo<!>

    fun <T> List<T>.testClassLiteral1() = a<T>::class
    fun <T> List<T>.testClassLiteral2() = b?::class
    fun <T> List<T>.testClassLiteral3() = b<T, Any>::class

    fun <T> List<T>.testUnresolved1() = <!UNRESOLVED_REFERENCE!>unresolved<!><T>::foo
    fun <T> List<T>.testUnresolved2() = a<unresolved>::foo
    fun <T> List<T>.testUnresolved3() = a<<!SYNTAX!><!>>::foo
    fun <T> List<T>.testUnresolved4() = <!UNRESOLVED_REFERENCE!>unresolved<!>?::foo
}