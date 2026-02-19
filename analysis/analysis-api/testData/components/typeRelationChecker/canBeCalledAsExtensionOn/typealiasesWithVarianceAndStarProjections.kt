interface BaseIn<in T>

fun <I: String, T: BaseIn<BaseIn<I>>> BaseIn<T>.ex<caret>t() {}

class ChildGenericAny<T: R, R: I, I: Any>: BaseIn<BaseIn<BaseIn<I>>>
typealias ChildGenericAliasAny = ChildGenericAny<*, *, *>

fun usage(x: ChildGenericAliasAny) {
    <expr>x</expr>
}