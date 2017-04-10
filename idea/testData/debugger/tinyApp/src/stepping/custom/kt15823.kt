package kt15823

object Some {
    val collection = mutableListOf<() -> Unit>()

    inline fun inlineWithReified(crossinline lambda: () -> Unit) {
        collection.add({ lambda() })
    }

    init {
        inlineWithReified {
            //Breakpoint!
            foo() // Will marked as (X), and never hit, but executes
        }
    }

    fun foo() {}

    fun magic() {
        collection.forEach { it.invoke() }
    }
}

fun main(args: Array<String>) {
    Some.magic()
}

// RESUME: 1