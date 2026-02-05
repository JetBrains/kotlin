// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// LANGUAGE: +JvmInlineMultiFieldValueClasses
package pack

typealias MyAlias<A> = List<A>

@JvmInline
value class <caret>Foo<T>(val alias: MyAlias<T>, val b: String)