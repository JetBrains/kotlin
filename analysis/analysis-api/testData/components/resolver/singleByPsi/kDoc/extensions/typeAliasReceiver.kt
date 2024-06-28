package test

class Foo
typealias FooAlias = Foo

fun Foo.fooExt() {}
fun FooAlias.fooAliasExt() {}

fun Any.anyExt() {}

class Other
fun Other.otherExt() {}

/**
 * [FooAlias.<caret_1>fooExt]
 *
 * [Foo.<caret_2>fooAliasExt]
 * [FooAlias.<caret_3>fooAliasExt]
 *
 * [FooAlias.<caret_4>anyExt]
 * [FooAlias.<caret_5>otherExt]
 */
fun usage() {}
