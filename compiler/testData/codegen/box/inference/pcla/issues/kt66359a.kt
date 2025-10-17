// ISSUE: KT-66359

// IGNORE_BACKEND_K1: ANY
// REASON: "Could not load module <Error module>"

fun box(): String {
    pcla {
        // GenericType<Xy, OTv> <: GenericType<*, OTv>
        constrainTypeVariable(
            // GenericType<Yv, ConcreteType> <: GenericType<Xy, OTv>
            produceConstraintSource()
        )
    }
    return "OK"
}


class TypeVariableOwner<T> {
    fun <X> constrainTypeVariable(constraintSource: GenericType<X, T>): GenericType<X, T> = constraintSource
}

fun <OT> pcla(lambda: TypeVariableOwner<OT>.() -> GenericType<*, OT>) {}

class GenericType<A, B>
class ConcreteType

fun <Y: Any> produceConstraintSource(): GenericType<Y, ConcreteType> = GenericType()
