// EXTRACTION_TARGET: property with getter
// WITH_RUNTIME

import java.util.*

// SIBLING:
class Foo<T> {
    val map = HashMap<String, T>()

    fun test(): T {
        return <selection>map[""]</selection>
    }
}