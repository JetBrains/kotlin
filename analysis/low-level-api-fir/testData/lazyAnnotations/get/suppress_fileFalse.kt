// QUERY: get: kotlin/Suppress
// RESOLVE_FILE
// WITH_STDLIB

@file:MyAnno(1 + 1)
package one

annotation class MyAnno(val i: Int)