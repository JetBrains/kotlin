// WITH_STDLIB
// FULL_JDK

interface IrType

interface TypeRemapper {
    fun enterScope(irTypeParametersContainer: IrTypeParametersContainer)
    fun remapType(type: IrType): IrType
    fun leaveScope()
}

interface IrTypeParametersContainer : IrDeclaration, IrDeclarationParent {
    var typeParameters: List<IrTypeParameter>
}

interface IrDeclaration
interface IrTypeParameter : IrDeclaration {
    val superTypes: MutableList<IrType>
}
interface IrDeclarationParent

class DeepCopyIrTreeWithSymbols(private val typeRemapper: TypeRemapper) {
    private fun copyTypeParameter(declaration: IrTypeParameter): IrTypeParameter = declaration

    fun IrTypeParametersContainer.copyTypeParametersFrom(other: IrTypeParametersContainer) {
        this.typeParameters = other.typeParameters.map {
            copyTypeParameter(it)
        }

        typeRemapper.withinScope(this) {
            for ((thisTypeParameter, otherTypeParameter) in this.typeParameters.zip(other.typeParameters)) {
                otherTypeParameter.superTypes.mapTo(thisTypeParameter.superTypes) {
                    typeRemapper.remapType(it)
                }
            }
        }
    }
}

inline fun <T> TypeRemapper.withinScope(irTypeParametersContainer: IrTypeParametersContainer, fn: () -> T): T {
    enterScope(irTypeParametersContainer)
    val result = fn()
    leaveScope()
    return result
}
