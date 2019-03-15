// WITH_RUNTIME

val x = listOf(1, 2, 3).<caret>map(
        // comment1
        Int::toString
).joinToString(
        // comment2
        prefix = "= ",
        // comment3
        separator = " + "
        // comment4
)