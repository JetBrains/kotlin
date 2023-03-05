// FIR_DISABLE_LAZY_RESOLVE_CHECKS
import kotlin.Self

@Self
class ReturnTypeWithTypeParameter<T> {
    fun returnType(): Self {
        return this
    }

    fun functionWithImplicitConstructor(): Self {
        val s = ReturnTypeWithTypeParameter<Int>()
        return this
    }

    fun functionWithManualConstructor(): Self {
        val s = ReturnTypeWithTypeParameter<Int, ReturnTypeWithTypeParameter<Int, *>>()
        return this
    }
}

@Self
class ReturnTypeWithMultipleTypeParameters<T, A, F> {
    fun returnType(): Self {
        return this
    }
}