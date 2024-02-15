// mypack.Usage
package mypack

typealias UnitTypeAlias = Unit
typealias NullableUnitTypeAlias = Unit?

class Usage {
    fun unitTypeAlias(): UnitTypeAlias {

    }

    fun unitTypeAliasAsNullable(): UnitTypeAlias? = null

    fun nullableUnitTypeAlias(): NullableUnitTypeAlias = null
    fun nullableUnitTypeAliasAsNullable(): NullableUnitTypeAlias? = null
}
