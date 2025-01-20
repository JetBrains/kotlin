// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-7972
// WITH_STDLIB
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

interface RecList<E : List<E>>
interface Box<T : List<T>> : RecList<T>

fun f2(a: RecList<*>) = a is Box<out List<Int>>

// For a new fresh variable `Var(S)` such that `Box<Var(S)> <: RecList<K>` (`K` from capturing),
// postilating `Var(S_ub) <: List<out Int>` may fail if
// `Var(S_ub)` references `S`. For example, here, `Var(S_ub) == List<Var(S_ub)>`, so
//           `Var(S_ub) <: List<out Int>`
// ==> `List<Var(S_ub)> <: List<out Int>`
// ==>       `Var(S_ub) <: Int`
// Also note that:
//         Box<Var(S)> <: RecList<K>
// ==> RecList<Var(S)> <: RecList<K> (the rule about consistent arguments in supertypes)
// ==> Var(S) == K
// So, we must make sure we get a contradiction in this example.
