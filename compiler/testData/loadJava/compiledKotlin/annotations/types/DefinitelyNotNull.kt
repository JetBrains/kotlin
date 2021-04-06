// !LANGUAGE: +DefinitelyNotNullTypeParameters
// COMPILER_ARGUMENTS: -XXLanguage:+DefinitelyNotNullTypeParameters
package test

fun <T> foo(x: T!!, y: List<T!!>, z: (T!!) -> T!!): T!! = x
