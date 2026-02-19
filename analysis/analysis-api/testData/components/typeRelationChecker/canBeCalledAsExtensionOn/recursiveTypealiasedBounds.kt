interface Base<T>

typealias BaseAlias<T> = Base<T>

fun <T: BaseAlias<T>> Base<T>.ex<caret>t() {}

class ChildGeneric<T>: Base<Base<Base<T>>>

typealias ChildGenericAlias<T> = ChildGeneric<T>

/**
 * [ChildGenericAlias.ext]
 */
fun usage(x: ChildGenericAlias) {
    <expr>x</expr>
}