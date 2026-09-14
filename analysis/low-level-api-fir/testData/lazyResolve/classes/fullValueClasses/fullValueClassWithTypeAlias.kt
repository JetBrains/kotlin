// LANGUAGE: +FullValueClasses
package pack

typealias MyAlias<A> = List<A>

value class F<caret>oo<T>(val alias: MyAlias<T>, val b: String)
