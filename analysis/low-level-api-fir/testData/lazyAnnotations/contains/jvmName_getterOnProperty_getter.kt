// QUERY: contains: kotlin.jvm/JvmName
// WITH_STDLIB

@get:JvmName("getter")
val property: Int = 0
    ge<caret>t() = 1