// FIR_IDENTICAL
// LANGUAGE: +RepeatableAnnotations +RepeatableAnnotationContainerConstraints
// FULL_JDK

import java.lang.annotation.Repeatable as R
import kotlin.annotation.AnnotationRetention.*

<!REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION_ERROR!>@R(C1::class)<!>
@Retention(RUNTIME)
annotation class A1
@Retention(BINARY)
annotation class C1(val value: Array<A1>)

<!REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION_ERROR!>@R(C2::class)<!>
@Retention(BINARY)
annotation class A2
@Retention(SOURCE)
annotation class C2(val value: Array<A2>)

<!REPEATABLE_CONTAINER_HAS_SHORTER_RETENTION_ERROR!>@R(C3::class)<!>
annotation class A3
@Retention(SOURCE)
annotation class C3(val value: Array<A3>)



@R(D1::class)
annotation class B1
@Retention(RUNTIME)
annotation class D1(val value: Array<B1>)

@R(D2::class)
@Retention(SOURCE)
annotation class B2
@Retention(BINARY)
annotation class D2(val value: Array<B2>)

@R(D3::class)
@Retention(BINARY)
annotation class B3
annotation class D3(val value: Array<B3>)
