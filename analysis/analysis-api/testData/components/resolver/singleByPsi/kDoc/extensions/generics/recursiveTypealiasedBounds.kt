interface Base<T>

typealias BaseAlias<T> = Base<T>

fun <T: Base<T>> BaseAlias<T>.ext1() {}
fun <T: BaseAlias<T>> Base<T>.ext2() {}

class Child: Base<Child>
class ChildGeneric<T>: Base<Base<Base<T>>>

typealias ChildAlias = Child
typealias ChildGenericAlias<T> = ChildGeneric<T>
typealias ChildGenericAliasInt = ChildGeneric<Int>

/**
 * [BaseAlias.ex<caret_1>t1]
 * [Child.ex<caret_2>t1]
 * [ChildAlias.ex<caret_3>t1]
 * [ChildGenericAlias.ex<caret_4>t1] - False positive due to KT-86935
 * [ChildGenericAliasInt.ex<caret_5>t1] - False positive due to KT-86935
 * [BaseAlias.ex<caret_6>t2]
 * [Child.ex<caret_7>t2]
 * [ChildAlias.ex<caret_8>t2]
 * [ChildGenericAlias.ex<caret_9>t2] - False positive due to KT-86935
 * [ChildGenericAliasInt.ex<caret_10>t2] - False positive due to KT-86935
 */
fun usage() {}
