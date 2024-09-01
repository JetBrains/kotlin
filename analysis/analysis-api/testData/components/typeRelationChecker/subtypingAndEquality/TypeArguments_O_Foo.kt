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

val v<caret_type2>2: Foo<List<Int>, Map<String, Int>> = O

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true

// SUPERCLASS_ID: test/Foo
// IS_CLASS_SUBTYPE: true
// IS_CLASS_SUBTYPE_LENIENT: true
