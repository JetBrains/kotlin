// ISSUE: KT-57839

fun <R> myRun(block: () -> R): R {
    return block()
}

interface Bar {
    val action: () -> Unit
}

val cardModel = myRun {
    object : Bar {
        override val action = {}
    }
}
