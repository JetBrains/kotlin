// FIR_DISABLE_LAZY_RESOLVE_CHECKS
import kotlin.Self

@Self
class ReturnTypeWithTypeParameter<T> {
    fun returnType(): Self {
        return this as Self
    }

    fun functionWithImplicitConstructor(): Self {
        val s = ReturnTypeWithTypeParameter<Int>()
        return this as Self
    }

    fun functionWithManualConstructor(): Self {
        val s = ReturnTypeWithTypeParameter<Int, ReturnTypeWithTypeParameter<Int, *>>()
        return this as Self
    }
}

@Self
class ReturnTypeWithMultipleTypeParameters<T, A, F> {
    fun returnType(): Self {
        return this as Self
    }
}