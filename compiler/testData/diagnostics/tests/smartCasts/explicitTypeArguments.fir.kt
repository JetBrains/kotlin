// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

interface Box<V>

fun <V> unbox(box: Box<out V>): Int = 42
fun <V> Box<out V>.unbox(): String = "42"

interface SuperType
interface SubTypeA: SuperType
interface SubTypeB: SuperType

fun <TypeParameter> test(
    boxedSuperType: Box<SuperType>,
    boxedSubTypeA: Box<SubTypeA>,
    boxedSubTypeB: Box<SubTypeB>,
    boxedTypeParameter: Box<TypeParameter>
) {
    if (boxedSuperType === boxedTypeParameter) {
        unbox(boxedTypeParameter)
        unbox<SuperType>(boxedTypeParameter)
        unbox<TypeParameter>(boxedTypeParameter)
        boxedTypeParameter.unbox()
        boxedTypeParameter.unbox<SuperType>()
        boxedTypeParameter.unbox<TypeParameter>()
        unbox(boxedSuperType)
        unbox<TypeParameter>(boxedSuperType)
        unbox<SuperType>(boxedSuperType)
        boxedSuperType.unbox()
        boxedSuperType.unbox<TypeParameter>()
        boxedSuperType.unbox<SuperType>()
    }
    if (boxedTypeParameter === boxedSuperType) {
        unbox(boxedTypeParameter)
        unbox<SuperType>(boxedTypeParameter)
        unbox<TypeParameter>(boxedTypeParameter)
        boxedTypeParameter.unbox()
        boxedTypeParameter.unbox<SuperType>()
        boxedTypeParameter.unbox<TypeParameter>()
        unbox(boxedSuperType)
        unbox<TypeParameter>(boxedSuperType)
        unbox<SuperType>(boxedSuperType)
        boxedSuperType.unbox()
        boxedSuperType.unbox<TypeParameter>()
        boxedSuperType.unbox<SuperType>()
    }
    if (boxedSubTypeA === boxedTypeParameter || boxedSubTypeB === boxedTypeParameter) {
        unbox(boxedTypeParameter)
        unbox<SuperType>(boxedTypeParameter)
        unbox<TypeParameter>(boxedTypeParameter)
        boxedTypeParameter.unbox()
        boxedTypeParameter.unbox<SuperType>()
        boxedTypeParameter.unbox<TypeParameter>()
    }
    if (boxedTypeParameter === boxedSubTypeA || boxedTypeParameter === boxedSubTypeB) {
        unbox(boxedTypeParameter)
        unbox<SuperType>(boxedTypeParameter)
        unbox<TypeParameter>(boxedTypeParameter)
        boxedTypeParameter.unbox()
        boxedTypeParameter.unbox<SuperType>()
        boxedTypeParameter.unbox<TypeParameter>()
    }
    if (boxedTypeParameter === boxedSubTypeA || boxedSubTypeB === boxedTypeParameter) {
        unbox(boxedTypeParameter)
        unbox<SuperType>(boxedTypeParameter)
        unbox<TypeParameter>(boxedTypeParameter)
        boxedTypeParameter.unbox()
        boxedTypeParameter.unbox<SuperType>()
        boxedTypeParameter.unbox<TypeParameter>()
    }
    if (boxedSubTypeA === boxedTypeParameter || boxedTypeParameter === boxedSubTypeB) {
        unbox(boxedTypeParameter)
        unbox<SuperType>(boxedTypeParameter)
        unbox<TypeParameter>(boxedTypeParameter)
        boxedTypeParameter.unbox()
        boxedTypeParameter.unbox<SuperType>()
        boxedTypeParameter.unbox<TypeParameter>()
    }
}
