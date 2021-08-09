// !LANGUAGE: +DefinitelyNotNullTypeParameters
// COMPILER_ARGUMENTS: -XXLanguage:+DefinitelyNotNullTypeParameters
package test

fun <T> foo(x: T & Any, y: List<T & Any>, z: (T & Any) -> T & Any): T & Any = x
