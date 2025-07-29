interface Base<T>
interface BaseIn<in T>

typealias BaseAlias<T> = Base<T>
typealias BaseAliasIn<T> = BaseIn<T>

fun <T> Base<T>.ext1() {}
fun <I: Int, T: BaseAlias<Base<I>>> Base<T>.ext2() {}
fun <I: String, T: BaseAlias<Base<I>>> Base<T>.ext3() {}

fun <T> BaseIn<T>.ext4() {}
fun <I: Int, T: BaseIn<BaseIn<I>>> BaseIn<T>.ext5() {}
fun <I: String, T: BaseIn<BaseIn<I>>> BaseIn<T>.ext6() {}

class ChildGenericInt<T: R, R: I, I: Int>: Base<BaseAlias<Base<I>>>
class ChildGenericAny<T: R, R: I, I: Any>: BaseIn<BaseIn<BaseIn<I>>>

typealias ChildGenericAliasInt = ChildGenericInt<*, *, *>
typealias ChildGenericAliasIntWithOneArgument = ChildGenericInt<Int, *, *>
typealias ChildGenericAliasAny = ChildGenericAny<*, *, *>

/**
 * [ChildGenericAliasInt.ex<caret_1>t1]
 * [ChildGenericAliasInt.ex<caret_2>t2]
 * [ChildGenericAliasInt.ex<caret_3>t3]
 * [ChildGenericAliasInt.ex<caret_4>t4]
 * [ChildGenericAliasInt.ex<caret_5>t5]
 * [ChildGenericAliasInt.ex<caret_6>t6]
 * [ChildGenericAliasAny.ex<caret_7>t1]
 * [ChildGenericAliasAny.ex<caret_8>t2]
 * [ChildGenericAliasAny.ex<caret_9>t3]
 * [ChildGenericAliasAny.ex<caret_10>t4]
 * [ChildGenericAliasAny.ex<caret_11>t5]
 * [ChildGenericAliasAny.ex<caret_12>t6]
 * [ChildGenericAliasIntWithOneArgument.ex<caret_13>t1]
 * [ChildGenericAliasIntWithOneArgument.ex<caret_14>t2]
 * [ChildGenericAliasIntWithOneArgument.ex<caret_15>t3]
 * [ChildGenericAliasIntWithOneArgument.ext<caret_16>4]
 * [ChildGenericAliasIntWithOneArgument.ex<caret_17>t5]
 * [ChildGenericAliasIntWithOneArgument.ex<caret_18>t6]
 */
fun usage() {}