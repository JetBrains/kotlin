// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

interface In<in T>
interface Out<out T>

typealias InAlias<T> = In<T>
typealias OutAlias<T> = Out<T>

typealias TestOutForIn<T> = In<<!CONFLICTING_PROJECTION!>out<!> T>
typealias TestInForOut<T> = Out<<!CONFLICTING_PROJECTION!>in<!> T>

typealias TestOutForInWithinAlias<T> = <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>InAlias<out T><!>
typealias TestInForOutWithinAlias<T> = <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>OutAlias<in T><!>

fun <T> testOutForInWithinResolvedType(x: <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>InAlias<out T><!>) {}
fun <T> testInForOutWithinResolvedType(x: <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>OutAlias<in T><!>) {}
