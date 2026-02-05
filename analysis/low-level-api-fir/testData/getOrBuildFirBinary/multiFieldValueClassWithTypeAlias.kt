// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Foo
// LANGUAGE: +JvmInlineMultiFieldValueClasses
package pack

typealias MyAlias<A> = List<A>

@JvmInline
value class Foo<T>(val alias: MyAlias<T>, val b: String)