// QUERY: contains: kotlin.jvm/JvmName
// WITH_STDLIB

val prop<caret>erty: Int = 0
    @JvmName("getter")
    get() = 1