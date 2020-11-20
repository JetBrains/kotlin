// !DIAGNOSTICS: -UNUSED_TYPEALIAS_PARAMETER

typealias WithVariance<<!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>in X<!>, <!VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED!>out Y<!>> = Int
typealias WithBounds1<T : T> = Int
typealias WithBounds2<X : Y, Y : X> = Int

typealias WithBounds3<X> <!SYNTAX!>where X : Any<!> = Int

val x: WithVariance<Int, Int> = 0
