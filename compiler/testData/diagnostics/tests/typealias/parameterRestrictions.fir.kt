// !DIAGNOSTICS: -UNUSED_TYPEALIAS_PARAMETER

typealias WithVariance<in X, out Y> = Int
typealias WithBounds1<T : T> = Int
typealias WithBounds2<X : Y, Y : X> = Int

typealias WithBounds3<X> <!SYNTAX!>where X : Any<!> = Int

val x: WithVariance<Int, Int> = 0
