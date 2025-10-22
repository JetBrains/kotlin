// ISSUE: KT-66359

// IGNORE_BACKEND_K1: ANY
// REASON: "Could not load module <Error module>"

fun box(): String {
    pcla {
        // GenericType<Xy, OTv> <: GenericType<*, OTv>
        constrainTypeVariable(
            // GenericType<Yv, ConcreteType> <: GenericType<Xy, OTv>
            ConstraintSource()
        )
    }
    return "OK"
}


class TypeVariableOwner<T: Any> {
    fun <X: Any> constrainTypeVariable(constraintSource: GenericType<X, T>): GenericType<X, T> = constraintSource
}

fun <OT: Any> pcla(lambda: TypeVariableOwner<OT>.() -> GenericType<*, OT>) {}

class GenericType<A: Any, B: Any>
class ConcreteType

typealias ConstraintSource<Y> = GenericType<Y, ConcreteType>
