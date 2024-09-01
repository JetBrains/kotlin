// KNM_K2_IGNORE
// KT-68007

package test

typealias StringAlias = String
typealias EntryAlias = Map.Entry<String, Int>
typealias FunctionAlias = (String) -> Int

typealias NestedStringAlias = StringAlias
typealias NestedFunctionAlias = FunctionAlias

typealias ParameterizedListAlias<A> = List<A>

typealias NestedListAliasWithTypeArgument = ParameterizedListAlias<String>
typealias NestedListAliasWithAliasTypeArgument = ParameterizedListAlias<StringAlias>
typealias NestedListAliasWithNestedAliasTypeArgument = ParameterizedListAlias<NestedStringAlias>

typealias NestedParameterizedListAlias<A> = ParameterizedListAlias<ParameterizedListAlias<A>>

typealias NullableStringAlias = String?
typealias NestedNullableStringAlias = NullableStringAlias
typealias NullableNestedStringAlias = StringAlias?

typealias NullableFunctionAlias = ((String) -> Int)?
typealias NestedNullableFunctionAlias = NullableFunctionAlias
typealias NullableNestedFunctionAlias = FunctionAlias?

class TypeAliasExpansion {
    fun entryToString(entry: EntryAlias): StringAlias = entry.key

    val functionAlias: FunctionAlias = { name -> name.length }

    val nestedStringAlias: NestedStringAlias = ""
    val nestedFunctionAlias: NestedFunctionAlias = { name -> name.length }

    val parameterizedListAliasWithString: ParameterizedListAlias<String> = emptyList()
    val parameterizedListAliasWithStringAlias: ParameterizedListAlias<StringAlias> = emptyList()
    val parameterizedListAliasWithNestedStringAlias: ParameterizedListAlias<NestedStringAlias> = emptyList()
    val parameterizedListAliasWithParameterizedListAliasWithStringAlias: ParameterizedListAlias<ParameterizedListAlias<StringAlias>> =
        emptyList()

    val nestedListAliasWithTypeArgument: NestedListAliasWithTypeArgument = emptyList()
    val nestedListAliasWithAliasTypeArgument: NestedListAliasWithAliasTypeArgument = emptyList()
    val nestedListAliasWithNestedAliasTypeArgument: NestedListAliasWithNestedAliasTypeArgument = emptyList()

    val nestedParameterizedListAliasWithString: NestedParameterizedListAlias<String> = emptyList()
    val nestedParameterizedListAliasWithStringAlias: NestedParameterizedListAlias<StringAlias> = emptyList()
    val nestedParameterizedListAliasWithNestedStringAlias: NestedParameterizedListAlias<NestedStringAlias> = emptyList()

    val stringAliasNullable: StringAlias? = null
    val nestedStringAliasNullable: NestedStringAlias? = null

    val nullableStringAlias: NullableStringAlias = null
    val nullableStringAliasNullable: NullableStringAlias? = null

    val nestedNullableStringAlias: NestedNullableStringAlias = null
    val nestedNullableStringAliasNullable: NestedNullableStringAlias? = null

    val nullableNestedStringAlias: NullableNestedStringAlias = null
    val nullableNestedStringAliasNullable: NullableNestedStringAlias? = null

    val functionAliasNullable: FunctionAlias? = null
    val nestedFunctionAliasNullable: NestedFunctionAlias? = null

    val nullableFunctionAlias: NullableFunctionAlias = null
    val nullableFunctionAliasNullable: NullableFunctionAlias? = null

    val nestedNullableFunctionAlias: NestedNullableFunctionAlias = null
    val nestedNullableFunctionAliasNullable: NestedNullableFunctionAlias? = null

    val nullableNestedFunctionAlias: NullableNestedFunctionAlias = null
    val nullableNestedFunctionAliasNullable: NullableNestedFunctionAlias? = null
}
