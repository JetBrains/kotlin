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

    fun <T> List<T>.testCallable1(): () -> Unit = <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!>a<T><!>::foo
    fun <T> List<T>.testCallable2(): () -> Unit = <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!>b<!>?::<!UNSAFE_CALL!>foo<!>
    fun <T> List<T>.testCallable3(): () -> Unit = <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!>b<T, Any><!>::<!UNSAFE_CALL!>foo<!>
    fun <T> List<T>.testCallable4(): () -> Unit = <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!>b<T><!>?::<!UNSAFE_CALL!>foo<!>

    fun <T> List<T>.testClassLiteral1() = <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!>a<T><!>::class
    fun <T> List<T>.<!KCLASS_WITH_NULLABLE_ARGUMENT_IN_SIGNATURE!>testClassLiteral2<!>() = <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS, EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>b<!>?::class
    fun <T> List<T>.<!KCLASS_WITH_NULLABLE_ARGUMENT_IN_SIGNATURE!>testClassLiteral3<!>() = <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS, EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>b<T, Any><!>::class

    fun <T> List<T>.testUnresolved1() = <!UNRESOLVED_REFERENCE!>unresolved<!><T>::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>
    fun <T> List<T>.testUnresolved2() = <!RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS!>a<<!UNRESOLVED_REFERENCE!>unresolved<!>><!>::foo
    fun <T> List<T>.testUnresolved3() = a<<!SYNTAX!><!>>::foo
    fun <T> List<T>.testUnresolved4() = <!UNRESOLVED_REFERENCE!>unresolved<!>?::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>
}