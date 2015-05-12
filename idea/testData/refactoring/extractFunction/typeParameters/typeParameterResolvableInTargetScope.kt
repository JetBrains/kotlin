// WITH_RUNTIME
// PARAM_TYPES: kotlin.String, Comparable<String>, CharSequence, kotlin.Any
// PARAM_DESCRIPTOR: value-parameter val l: kotlin.String defined in Foo.test

import java.util.*

class Foo<T> {
    val map = HashMap<String, T>()

    fun test(l: String): T {
        return <selection>map[l]</selection>
    }
}