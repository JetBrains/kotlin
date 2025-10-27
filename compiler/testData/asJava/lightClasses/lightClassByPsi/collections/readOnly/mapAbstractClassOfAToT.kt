// WITH_STDLIB
package test

class A

abstract class ATMap<T> : Map<A, T>

abstract class ATMap2<T> : Map<A, T> by emptyMap<A, T>()
