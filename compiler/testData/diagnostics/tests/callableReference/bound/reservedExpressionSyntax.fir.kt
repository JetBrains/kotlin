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

    fun <T> List<T>.testCallable1(): () -> Unit = a::foo
    fun <T> List<T>.testCallable1a(): () -> Unit = <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>a<!><T>::foo
    fun <T> List<T>.testCallable2(): () -> Unit = <!SAFE_CALLABLE_REFERENCE_CALL!>b?::<!UNSAFE_CALL!>foo<!><!>
    fun <T> List<T>.testCallable3(): () -> Unit = <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>b<!><T, Any>::<!UNSAFE_CALL!>foo<!>
    fun <T> List<T>.testCallable4(): () -> Unit = <!SAFE_CALLABLE_REFERENCE_CALL!><!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>b<!><T>?::<!UNSAFE_CALL!>foo<!><!>

    fun <T> List<T>.testClassLiteral1() = a::class
    fun <T> List<T>.testClassLiteral1a() = <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>a<!><T>::class
    fun <T> List<T>.testClassLiteral2() = <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>b<!>?::class
    fun <T> List<T>.testClassLiteral3() = <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!><!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>b<!><T, Any><!>::class

    fun <T> List<T>.testUnresolved1() = <!UNRESOLVED_REFERENCE!>unresolved<!><T>::foo
    fun <T> List<T>.testUnresolved2() = <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>a<!><<!UNRESOLVED_REFERENCE!>unresolved<!>>::foo
    fun <T> List<T>.testUnresolved3() = a<<!SYNTAX!><!>>::foo
    fun <T> List<T>.testUnresolved4() = <!SAFE_CALLABLE_REFERENCE_CALL!><!UNRESOLVED_REFERENCE!>unresolved<!>?::foo<!>
}
