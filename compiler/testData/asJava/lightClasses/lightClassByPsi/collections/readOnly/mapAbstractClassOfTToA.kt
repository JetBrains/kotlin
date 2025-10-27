// WITH_STDLIB
package test

class A

abstract class TAMap<T> : Map<T, A>

abstract class TAMap2<T> : Map<T, A> by emptyMap<T, A>()
