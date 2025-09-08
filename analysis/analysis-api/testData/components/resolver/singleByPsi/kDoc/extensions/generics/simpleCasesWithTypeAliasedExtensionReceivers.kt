interface Base<T>

fun <T> ChildGenericAlias<T>.ext1() {}
fun ChildGenericAliasWithStarProjection.ext2() {}
fun ChildGenericAliasWithNothing.ext3() {}
fun ChildGenericAliasWithAnotherNothingAlias.ext4() {}

typealias ChildGenericAlias<T> = Base<T>
typealias ChildGenericAliasWithStarProjection = Base<*>
typealias ChildGenericAliasWithNothing = Base<Nothing>
typealias ChildGenericAliasWithAnotherNothingAlias = ChildGenericAlias<ChildGenericAliasWithNothing>

typealias DoubleNestedBaseStar = Base<Base<*>>
typealias DoubleNestedBaseNothing = Base<Base<Nothing>>

/**
 * [Base.ext<caret_1>1]
 * [Base.ext<caret_2>2]
 * [Base.ext<caret_3>3]
 * [Base.ext<caret_4>4]
 *
 * [DoubleNestedBaseStar.ext<caret_5>1]
 * [DoubleNestedBaseStar.ext<caret_6>2]
 * [DoubleNestedBaseStar.ext<caret_7>3]
 * [DoubleNestedBaseStar.ext<caret_8>4]
 *
 * [DoubleNestedBaseNothing.ext<caret_9>1]
 * [DoubleNestedBaseNothing.ext<caret_10>2]
 * [DoubleNestedBaseNothing.ext<caret_11>3]
 * [DoubleNestedBaseNothing.ext<caret_12>4]
 */
fun foo() {}