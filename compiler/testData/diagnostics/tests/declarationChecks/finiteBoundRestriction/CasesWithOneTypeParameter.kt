// FIR_IDENTICAL
interface A0<T : A0<T>>
interface A1<<!FINITE_BOUNDS_VIOLATION!>T : A1<*><!>>
interface A2<<!FINITE_BOUNDS_VIOLATION!>T : A2<out T><!>>
interface A3<<!FINITE_BOUNDS_VIOLATION!>T : A3<in T><!>>
interface A4<<!FINITE_BOUNDS_VIOLATION!>T : A4<*>?<!>>

interface B0<<!FINITE_BOUNDS_VIOLATION!>T : B1<*><!>>
interface B1<<!FINITE_BOUNDS_VIOLATION!>T : B0<*><!>>

interface AA<<!FINITE_BOUNDS_VIOLATION!>T: AA<*><!>>
interface BB<S : List<AA<*>>>

interface A<T: List<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><T, T, T><!>>

class X<Y>
class D<T : X<in X<out X<T>>>>
