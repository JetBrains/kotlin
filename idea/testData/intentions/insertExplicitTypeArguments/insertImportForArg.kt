fun foo(c: kotlin.support.AbstractIterator<kotlin.properties.ObservableProperty<Int>>) {
    ba<caret>r(c)
}

fun <T> bar(t: T): T = t

// WITH_RUNTIME