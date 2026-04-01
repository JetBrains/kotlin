// LANGUAGE: +CompanionBlocksAndExtensions
// WITH_STDLIB

package test

enum class E {
    Entry;

    companion {
        context(s: String)
        val entries get() = listOf(Entry)

        context(s: String)
        fun values() = arrayOf(Entry)

        context(s: String)
        fun valueOf(x: String) = Entry
    }
}