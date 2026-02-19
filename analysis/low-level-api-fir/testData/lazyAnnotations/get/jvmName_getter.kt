// QUERY: get: kotlin.jvm/JvmName
// WITH_STDLIB

val property: Int = 0
    @JvmName("getter")
    ge<caret>t() = 1