package test

fun interface Box<T> {
    fun provide(): T
}

typealias Alias<TT> = Box<TT>

fun usage() {
    <caret>Alias<_> { "OK" }
}
