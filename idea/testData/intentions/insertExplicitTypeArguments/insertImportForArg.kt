fun foo(c: kotlin.support.AbstractIterator<kotlin.concurrent.FunctionalList<Int>>) {
    ba<caret>r(c)
}

fun <T> bar(t: T): T = t

// WITH_RUNTIME