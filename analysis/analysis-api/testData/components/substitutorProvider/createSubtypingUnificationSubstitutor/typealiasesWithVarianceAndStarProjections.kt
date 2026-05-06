interface BaseIn<in S>

fun <I: String, T: BaseIn<BaseIn<I>>> BaseIn<T>.ext() {
    th<caret_1_target>is
}

class ChildGenericAny<K: R, R: M, M: Any>: BaseIn<BaseIn<BaseIn<K>>>
typealias ChildGenericAliasAny = ChildGenericAny<*, *, *>

fun usage(xx: ChildGenericAliasAny) {
    x<caret_1_base>x
}
