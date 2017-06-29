// WITH_RUNTIME

val someList = listOf("alpha", "beta").<caret>mapIndexedNotNullTo(destination = hashSetOf()) { index, value -> index + value.length }