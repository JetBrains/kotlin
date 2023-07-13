// WITH_STDLIB
// LANGUAGE: +ProhibitConstructorCallOnFunctionalSupertype
// ISSUE: KT-46344

abstract class A : () -> Int<!NO_CONSTRUCTOR!>()<!>
abstract class B : (() -> Int)<!NO_CONSTRUCTOR!>()<!>
abstract class C : Function0<Int><!NO_CONSTRUCTOR!>()<!>
abstract class D : suspend () -> Int<!NO_CONSTRUCTOR!>()<!>
abstract class E : (suspend () -> Int)<!NO_CONSTRUCTOR!>()<!>
abstract class F : kotlin.coroutines.SuspendFunction0<Int><!NO_CONSTRUCTOR!>()<!>

interface IA : <!SUPERTYPE_INITIALIZED_IN_INTERFACE!>() -> Int<!><!NO_CONSTRUCTOR!>()<!>
interface IB : <!SUPERTYPE_INITIALIZED_IN_INTERFACE!>(() -> Int)<!><!NO_CONSTRUCTOR!>()<!>
interface IC : <!SUPERTYPE_INITIALIZED_IN_INTERFACE!>Function0<Int><!><!NO_CONSTRUCTOR!>()<!>
interface ID : <!SUPERTYPE_INITIALIZED_IN_INTERFACE!>suspend () -> Int<!><!NO_CONSTRUCTOR!>()<!>
interface IE : <!SUPERTYPE_INITIALIZED_IN_INTERFACE!>(suspend () -> Int)<!><!NO_CONSTRUCTOR!>()<!>
interface IF : <!SUPERTYPE_INITIALIZED_IN_INTERFACE!>kotlin.coroutines.SuspendFunction0<Int><!><!NO_CONSTRUCTOR!>()<!>
