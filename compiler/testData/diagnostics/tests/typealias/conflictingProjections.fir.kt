// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

interface In<in T>
interface Out<out T>

typealias InAlias<T> = In<T>
typealias OutAlias<T> = Out<T>

typealias TestOutForIn<T> = In<<!CONFLICTING_PROJECTION!>out<!> T>
typealias TestInForOut<T> = Out<<!CONFLICTING_PROJECTION!>in<!> T>

typealias TestOutForInWithinAlias<T> = InAlias<out T>
typealias TestInForOutWithinAlias<T> = OutAlias<in T>

fun <T> testOutForInWithinResolvedType(x: InAlias<<!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>out<!> T>) {}
fun <T> testInForOutWithinResolvedType(x: OutAlias<<!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>in<!> T>) {}
