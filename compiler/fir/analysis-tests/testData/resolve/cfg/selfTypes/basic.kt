// FIR_DISABLE_LAZY_RESOLVE_CHECKS
import kotlin.Self

@Self
class JustSelfAnnotation {
    fun anyFun(): String = "string"
}

@Self
class ReturnType {
    fun returnTypeWithVal(): Self {
        val res: Self  = this
        return res
    }
}

@Self
class SelfWithSelfVariable {
    fun returnType(): Self {
        val Self: Self = this
        return Self
    }
}

@Self
class SelfTypeWithSelfFunction {
    fun Self(): Self {
        return this
    }
}

@Self
class SelfWithFunctionReturningItself {
    fun ret(): SelfWithFunctionReturningItself {
        return this
    }
}

@Self
class SelfWithFunctionReturningItselfAndTypeParameter<T> {
    fun ret(): SelfWithFunctionReturningItselfAndTypeParameter<T> {
        return this
    }
}