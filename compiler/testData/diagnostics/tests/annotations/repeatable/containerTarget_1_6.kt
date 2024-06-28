// FIR_IDENTICAL
// LANGUAGE: +RepeatableAnnotations +RepeatableAnnotationContainerConstraints
// FULL_JDK

import java.lang.annotation.Repeatable as R
import kotlin.annotation.AnnotationTarget.*

<!REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET_ERROR!>@R(C1::class)<!>
annotation class A1
@Target(FILE)
annotation class C1(val value: Array<A1>)

<!REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET_ERROR!>@R(C2::class)<!>
@Target(CLASS)
annotation class A2
@Target(CLASS, FUNCTION)
annotation class C2(val value: Array<A2>)

<!REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET_ERROR!>@R(C3::class)<!>
@Target(TYPE)
annotation class A3
annotation class C3(val value: Array<A3>)

<!REPEATABLE_CONTAINER_TARGET_SET_NOT_A_SUBSET_ERROR!>@R(C4::class)<!>
@Target(ANNOTATION_CLASS)
annotation class A4
@Target(CLASS)
annotation class C4(val value: Array<A4>)




@R(D1::class)
annotation class B1
@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER) // Default target set
annotation class D1(val value: Array<B1>)

@R(D2::class)
@Target(CLASS, FILE)
annotation class B2
@Target(CLASS)
annotation class D2(val value: Array<B2>)

@R(D3::class)
@Target(CLASS)
annotation class B3
@Target(ANNOTATION_CLASS)
annotation class D3(val value: Array<B3>)

@R(D4::class)
@Target(TYPE)
annotation class B4
@Target(ANNOTATION_CLASS, CLASS, TYPE_PARAMETER)
annotation class D4(val value: Array<B4>)
