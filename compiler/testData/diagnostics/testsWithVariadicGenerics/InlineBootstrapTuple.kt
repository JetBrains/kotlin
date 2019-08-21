// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

package kotlin;

class Tuple<vararg Ts>(vararg val elements: *Ts)

fun <vararg Ts> withArrayOfTuples(vararg tuples: *Ts) {}
