// WITH_STDLIB
// LANGUAGE: -ProhibitConstructorCallOnFunctionalSupertype
// ISSUE: KT-46344

abstract class A : () -> Int<!NO_CONSTRUCTOR_WARNING!>()<!>
abstract class B : (() -> Int)<!NO_CONSTRUCTOR_WARNING!>()<!>
abstract class C : Function0<Int><!NO_CONSTRUCTOR!>()<!>
abstract class D : suspend () -> Int<!NO_CONSTRUCTOR_WARNING!>()<!>
abstract class E : (suspend () -> Int)<!NO_CONSTRUCTOR_WARNING!>()<!>
abstract class F : kotlin.coroutines.SuspendFunction0<Int><!NO_CONSTRUCTOR!>()<!>

interface IA : () -> Int<!SUPERTYPE_INITIALIZED_IN_INTERFACE!>()<!>
interface IB : (() -> Int)<!SUPERTYPE_INITIALIZED_IN_INTERFACE!>()<!>
interface IC : Function0<Int><!SUPERTYPE_INITIALIZED_IN_INTERFACE!>()<!>
interface ID : suspend () -> Int<!SUPERTYPE_INITIALIZED_IN_INTERFACE!>()<!>
interface IE : (suspend () -> Int)<!SUPERTYPE_INITIALIZED_IN_INTERFACE!>()<!>
interface IF : kotlin.coroutines.SuspendFunction0<Int><!SUPERTYPE_INITIALIZED_IN_INTERFACE!>()<!>
