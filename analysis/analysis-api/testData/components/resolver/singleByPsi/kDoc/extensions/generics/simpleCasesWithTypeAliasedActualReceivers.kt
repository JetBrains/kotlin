interface Base<T>

fun <T> Base<T>.ext() {}
fun <T: Base<T>> Base<T>.extRecursive() {}
fun Base<Nothing>.extNothing() {}
fun Base<*>.extStar() {}

typealias ChildGenericAlias<T> = Base<T>
typealias ChildGenericAliasWithStarProjection = Base<*>
typealias ChildGenericAliasWithNothing = Base<Nothing>
typealias ChildGenericAliasWithAnotherNothingAlias = Base<ChildGenericAliasWithNothing>

/**
 * [ChildGenericAlias.ex<caret_1>t]
 * [ChildGenericAlias.extRe<caret_2>cursive]
 * [ChildGenericAlias.extN<caret_3>othing]
 * [ChildGenericAlias.ext<caret_4>Star]
 *
 * [ChildGenericAliasWithStarProjection.e<caret_5>xt]
 * [ChildGenericAliasWithStarProjection.extR<caret_6>ecursive]
 * [ChildGenericAliasWithStarProjection.extNot<caret_7>hing]
 * [ChildGenericAliasWithStarProjection.extS<caret_8>tar]
 *
 * [ChildGenericAliasWithNothing.ex<caret_9>t]
 * [ChildGenericAliasWithNothing.extRecu<caret_10>rsive]
 * [ChildGenericAliasWithNothing.extNot<caret_11>hing]
 * [ChildGenericAliasWithNothing.ext<caret_12>Star]
 *
 * [ChildGenericAliasWithAnotherNothingAlias.ex<caret_13>t]
 * [ChildGenericAliasWithAnotherNothingAlias.extR<caret_14>ecursive]
 * [ChildGenericAliasWithAnotherNothingAlias.extN<caret_15>othing]
 * [ChildGenericAliasWithAnotherNothingAlias.ext<caret_16>Star]
 */
fun foo() {}