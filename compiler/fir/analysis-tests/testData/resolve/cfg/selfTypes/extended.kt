// FIR_DISABLE_LAZY_RESOLVE_CHECKS
import kotlin.Self

@Self
interface SelfTypeParameterInterface

@Self
class SelfTypeAsTypeParameterInExtends : SelfTypeParameterInterface<Self> {

    fun returnType(): Self {
        return this as Self
    }
}

@Self
class SelfTypeWithSelfFunction {
    fun Self(): Self {
        return this as Self
    }
}

interface WithTypeParameter<out T> {
    fun foo(): T
}

@Self
class ExtendingInterfaceWithTypeParameter : WithTypeParameter<Self> {
    override fun foo(): Self {
        return this as Self
    }
}

@Self
abstract class AbstractClassWithSelf<T> {
    abstract fun self(): Self
}

class ExtendingAbstractClassWithSelf<T> : AbstractClassWithSelf<T, ExtendingAbstractClassWithSelf<T>>() {
    override fun self(): ExtendingAbstractClassWithSelf<T> {
        return this
    }
}