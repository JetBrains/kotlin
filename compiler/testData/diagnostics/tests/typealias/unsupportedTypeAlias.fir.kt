// !LANGUAGE: -TypeAliases

class C

typealias S = String
typealias L<T> = List<T>
typealias CA = C
typealias Unused = Any

val test1: S = ""

fun test2(x: L<S>) = x

class Test3 : CA()