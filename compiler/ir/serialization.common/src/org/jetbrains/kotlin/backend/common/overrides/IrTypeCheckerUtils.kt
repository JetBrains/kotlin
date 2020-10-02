package org.jetbrains.kotlin.backend.common.overrides

import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.types.IrTypeCheckerContext
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

open class IrTypeCheckerContextWithAdditionalAxioms(
    override val irBuiltIns: IrBuiltIns,
    firstParameters: List<IrTypeParameter>,
    secondParameters: List<IrTypeParameter>
) : IrTypeCheckerContext(irBuiltIns) {
    init {
        assert(firstParameters.size == secondParameters.size) {
            "different length of type parameter lists: $firstParameters vs $secondParameters"
        }
    }

    private val firstTypeParameterConstructors = firstParameters.map { it.symbol }
    private val secondTypeParameterConstructors = secondParameters.map { it.symbol }
    private val matchingTypeConstructors = firstTypeParameterConstructors.zip(secondTypeParameterConstructors).toMap()

    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        if (super.areEqualTypeConstructors(c1, c2)) return true
        if (matchingTypeConstructors[c1] == c2 || matchingTypeConstructors[c2] == c1) return true
        return false
    }
}