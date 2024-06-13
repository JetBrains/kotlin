// FIR_IDENTICAL
// LANGUAGE: +RepeatableAnnotations +RepeatableAnnotationContainerConstraints
// FULL_JDK

import java.lang.annotation.Repeatable as R

<!REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY_ERROR!>@R(C1::class)<!>
annotation class A1
annotation class C1

<!REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY_ERROR!>@R(C2::class)<!>
annotation class A2
annotation class C2(val value: A2)

<!REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY_ERROR!>@R(C3::class)<!>
annotation class A3
annotation class C3(val value: Array<String>)

<!REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY_ERROR!>@R(C4::class)<!>
annotation class A4
annotation class C4(val notValue: Array<A4>)

<!REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER_ERROR!>@R(C5::class)<!>
annotation class A5
annotation class C5(val value: Array<A5>, val irrelevant: String)

<!REPEATABLE_CONTAINER_HAS_NON_DEFAULT_PARAMETER_ERROR!>@R(C6::class)<!>
annotation class A6
annotation class C6(val irrelevant: Double, val value: Array<A6> = [])

@R(A7::class)
annotation class A7(val value: Array<A7>)



@R(D1::class)
annotation class B1
annotation class D1(val value: Array<B1>)

@R(D2::class)
annotation class B2
annotation class D2(val value: Array<B2> = [])

@R(D3::class)
annotation class B3
annotation class D3(val value: Array<B3>, val other1: String = "", val other2: Int = 42)

@R(D4::class)
annotation class B4
annotation class D4(val value1: Array<B4> = [], val value: Array<B4>)
