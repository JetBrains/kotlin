// WITH_STDLIB
// LANGUAGE: +ProhibitConstructorCallOnFunctionalSupertype
// ISSUE: KT-46344

abstract class A : <!UNRESOLVED_REFERENCE!>() -> Int<!>()
abstract class B : <!UNRESOLVED_REFERENCE!>(() -> Int)<!>()
abstract class C : <!UNRESOLVED_REFERENCE!>Function0<Int><!>()
abstract class D : <!UNRESOLVED_REFERENCE!>suspend () -> Int<!>()
abstract class E : <!UNRESOLVED_REFERENCE!>(suspend () -> Int)<!>()
abstract class F : <!UNRESOLVED_REFERENCE!>kotlin.coroutines.SuspendFunction0<Int><!>()

interface IA : <!SUPERTYPE_INITIALIZED_IN_INTERFACE!>() -> Int<!>()
interface IB : <!SUPERTYPE_INITIALIZED_IN_INTERFACE!>(() -> Int)<!>()
interface IC : <!SUPERTYPE_INITIALIZED_IN_INTERFACE!>Function0<Int><!>()
interface ID : <!SUPERTYPE_INITIALIZED_IN_INTERFACE!>suspend () -> Int<!>()
interface IE : <!SUPERTYPE_INITIALIZED_IN_INTERFACE!>(suspend () -> Int)<!>()
interface IF : <!SUPERTYPE_INITIALIZED_IN_INTERFACE!>kotlin.coroutines.SuspendFunction0<Int><!>()
