// WITH_RUNTIME
// PARAM_TYPES: Foo<T>
// PARAM_TYPES: kotlin.String, Comparable<String>, CharSequence, java.io.Serializable, kotlin.Any
// PARAM_DESCRIPTOR: internal final class Foo<T> defined in root package
// PARAM_DESCRIPTOR: value-parameter val l: kotlin.String defined in Foo.test

import java.util.*

// SIBLING:
internal class Foo<T> {
    val map = HashMap<String, T>()

    fun test(l: String): T {
        return <selection>map[l]</selection>
    }
}