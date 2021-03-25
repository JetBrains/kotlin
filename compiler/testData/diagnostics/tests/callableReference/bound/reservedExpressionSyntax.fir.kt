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
    fun <T> List<T>.testCallable2(): () -> Unit = b?::<!UNRESOLVED_REFERENCE!>foo<!>
    fun <T> List<T>.testCallable3(): () -> Unit = b<T, Any>::<!UNRESOLVED_REFERENCE!>foo<!>
    fun <T> List<T>.testCallable4(): () -> Unit = b<T>?::<!UNRESOLVED_REFERENCE!>foo<!>

    fun <T> List<T>.testClassLiteral1() = a<T>::class
    fun <T> List<T>.testClassLiteral2() = <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>b<!>?::class
    fun <T> List<T>.testClassLiteral3() = <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>b<T, Any><!>::class

    fun <T> List<T>.testUnresolved1() = <!UNRESOLVED_REFERENCE!>unresolved<!><T>::foo
    fun <T> List<T>.testUnresolved2() = a<<!UNRESOLVED_REFERENCE!>unresolved<!>>::foo
    fun <T> List<T>.testUnresolved3() = a<<!SYNTAX!><!>>::foo
    fun <T> List<T>.testUnresolved4() = <!UNRESOLVED_REFERENCE!>unresolved<!>?::foo
}
