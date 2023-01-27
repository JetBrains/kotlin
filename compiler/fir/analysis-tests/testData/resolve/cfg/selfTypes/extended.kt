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

interface ClassWithTypeParameter<out T> {
    fun foo(): T
}

@Self
class ClassExtendingInterfaceWithTypeParameter : ClassWithTypeParameter<Self> {
    override fun foo(): Self {
        return this as Self
    }
}