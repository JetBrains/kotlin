// WITH_STDLIB

interface Indexer {
    fun indexing()
}

interface MyBuilder

fun build(builder: MyBuilder) {
}

fun test(): Indexer {
    return object : Indexer {
        override fun indexing() {
            class TagData(val name: String) {
            }

            fun foo() {
                build(object : MyBuilder {
                    val tags<caret> = mutableListOf<TagData>()
                })
            }
        }
    }
}