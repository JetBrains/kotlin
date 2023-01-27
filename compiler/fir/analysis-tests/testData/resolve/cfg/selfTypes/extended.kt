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

@Self
open class SelfClassWithNested {
    class NestedClassExtendingOuterSelf : SelfClassWithNested<NestedClassExtendingOuterSelf>() {
        fun foo(): NestedClassExtendingOuterSelf {
            return this
        }
    }
}

class OuterClassWithNested {
    @Self
    open class SelfNested {

    }

    class NestedExtendingSelfNested : SelfNested<NestedExtendingSelfNested>() {
        fun foo(): NestedExtendingSelfNested {
            return this
        }
    }
}