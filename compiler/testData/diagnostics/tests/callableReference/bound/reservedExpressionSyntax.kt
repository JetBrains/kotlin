// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
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
    fun <T> List<T>.testCallable1a(): () -> Unit = a<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><T><!>::foo
    fun <T> List<T>.testCallable2(): () -> Unit = <!SAFE_CALLABLE_REFERENCE_CALL!>b?::<!UNSAFE_CALLABLE_REFERENCE!>foo<!><!>
    fun <T> List<T>.testCallable3(): () -> Unit = b<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><T, Any><!>::<!UNSAFE_CALLABLE_REFERENCE!>foo<!>
    fun <T> List<T>.testCallable4(): () -> Unit = <!SAFE_CALLABLE_REFERENCE_CALL!>b<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><T><!>?::<!UNSAFE_CALLABLE_REFERENCE!>foo<!><!>

    fun <T> List<T>.testClassLiteral1() = a::class
    fun <T> List<T>.testClassLiteral1a() = a<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><T><!>::class
    fun <T> List<T>.testClassLiteral2() = <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>b<!>?::class
    fun <T> List<T>.testClassLiteral3() = <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>b<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><T, Any><!><!>::class

    fun <T> List<T>.testUnresolved1() = <!UNRESOLVED_REFERENCE!>unresolved<!><T>::foo
    fun <T> List<T>.testUnresolved2() = a<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><<!UNRESOLVED_REFERENCE!>unresolved<!>><!>::foo
    fun <T> List<T>.testUnresolved3() = a<<!SYNTAX!><!>>::foo
    fun <T> List<T>.testUnresolved4() = <!SAFE_CALLABLE_REFERENCE_CALL!><!UNRESOLVED_REFERENCE!>unresolved<!>?::foo<!>
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, classReference, funWithExtensionReceiver,
functionDeclaration, functionalType, getter, nullableType, objectDeclaration, outProjection, propertyDeclaration,
propertyWithExtensionReceiver, typeParameter */
