// WITH_REFLECT
// LANGUAGE: -ProhibitCyclesInAnnotations
// ISSUE: KT-47932

import kotlin.reflect.KClass

annotation class X(val value: X) // error
annotation class Y(val value: Array<Y>) // error

annotation class Z1(val a: Z2, val b: Z2) // error
annotation class Z2(val value: Z1) // error

annotation class A(val x: KClass<A>) // OK
annotation class B(val x: KClass<B>) // OK
annotation class C(val b: B) // OK