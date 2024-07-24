// WITH_STDLIB
package test

interface Thing<X>

interface Foo<A, B>

interface Bar<A, B, C> : Foo<A, Pair<B, C>>, Thing<B>

interface Baz<X> : Bar<X, String, X>, Thing<String>

class Y<U> : Baz<U>

class Z<V> : Bar<List<V>, Int, String>

object O : Foo<List<Int>, Map<String, Int>>

val v<caret_type1>1: O = O

val v<caret_type2>2: Bar<List<String>, Int, String> = Z<String>()

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: false

// SUPERCLASS_ID: test/Bar
// IS_CLASS_SUBTYPE: false
// IS_CLASS_SUBTYPE_LENIENT: false
