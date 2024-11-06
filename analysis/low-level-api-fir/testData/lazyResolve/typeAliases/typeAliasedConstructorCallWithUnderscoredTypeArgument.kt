package test

class Box<T>(val value: T)

typealias Alias<TT> = Box<TT>

fun usage() {
    <caret>Alias<_>("OK")
}
