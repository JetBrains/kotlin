// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
// KT-3372 Use upper bound in type argument inference

import java.util.HashSet

fun <T, C: MutableCollection<T>> Iterable<T>.toCollection(result: C) : C = throw Exception()

fun test(list: List<Int>) {
    val set = list.toCollection(HashSet())
    set checkType { _<HashSet<Int>>() }
}