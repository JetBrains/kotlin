// WITH_STDLIB
// ISSUE: KT-64692

// MODULE: lib
// FILE: lib.kt
class MyMap : AbstractMap<Int, Int>() {
    override val entries = emptySet<Map.Entry<Int, Int>>()

    // clash with stdlib internal function
    fun containsEntry(entry: Map.Entry<*, *>?) = true
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    val result = MyMap().containsEntry(object : Map.Entry<Int, Int> {
        override val key = 123
        override val value = 113
    })
    return when (result) {
        true -> "OK"
        false -> "Fail"
    }
}
