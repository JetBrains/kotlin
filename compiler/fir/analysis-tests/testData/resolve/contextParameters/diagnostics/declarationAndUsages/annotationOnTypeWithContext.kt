// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-73149
import kotlin.annotation.AnnotationTarget.TYPE

@Target(TYPE)
annotation class AnnotationWithTypeTarget

@Target(TYPE)
annotation class AnnotationWithConstructor(val k: String)

class A

fun annotationOnContextType(a: context(<!SYNTAX!>@<!>AnnotationWithTypeTarget<!SYNTAX!><!> A<!SYNTAX!><!>)<!SYNTAX!>(<!><!SYNTAX!>)<!><!SYNTAX!>-><!><!SYNTAX!>Unit<!><!SYNTAX!>)<!> <!FUNCTION_DECLARATION_WITH_NO_NAME!><!SYNTAX!><!>{ }<!>

<!NON_MEMBER_FUNCTION_NO_BODY!>fun annotationWithConstructorOnContextType(a: context(<!SYNTAX!>@<!>AnnotationWithConstructor<!SYNTAX!><!>(<!SYNTAX!>"<!><!SYNTAX!><!SYNTAX!><!>"<!><!SYNTAX!>)<!> A)<!><!SYNTAX!>(<!><!SYNTAX!>)<!><!SYNTAX!>-><!><!SYNTAX!>Unit<!><!SYNTAX!>)<!> <!FUNCTION_DECLARATION_WITH_NO_NAME!><!SYNTAX!><!>{ }<!>

fun annotationOnTypeWithContext(a: @AnnotationWithTypeTarget context(A)()->Unit) { }

<!NON_MEMBER_FUNCTION_NO_BODY!>fun annotationOnFunWithMoreThenOneContextType(a: context(<!SYNTAX!>@<!>AnnotationWithTypeTarget<!SYNTAX!><!> A<!SYNTAX!><!>, <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!><!WRONG_ANNOTATION_TARGET!>@AnnotationWithTypeTarget<!> String<!>)<!><!SYNTAX!>(<!><!SYNTAX!>)<!><!SYNTAX!>-><!><!SYNTAX!>Unit<!><!SYNTAX!>)<!> <!FUNCTION_DECLARATION_WITH_NO_NAME!><!SYNTAX!><!>{ }<!>

fun annotationOnValueParameterWithContextType(a: context(A)(@AnnotationWithTypeTarget A)->Unit) { }

fun annotationOnExtensionParameterWithContextType(a: context(A)(@AnnotationWithTypeTarget A).()->Unit) { }
