// FIR_DISABLE_LAZY_RESOLVE_CHECKS
import kotlin.Self

@Self
class JustSelfAnnotation {
    fun anyFun(): String = "string"
}

@Self
class ReturnType {
    fun returnTypeWithVal(): Self {
        val res: Self  = this as Self
        return res
    }
}

@Self
class ReturnTypeWithTypeParameter<T> {
    fun returnType(): Self {
        return this as Self
    }

    fun functionWithConstruct(): Self {
        val s = ReturnTypeWithTypeParameter<Int, ReturnTypeWithTypeParameter<Int, *>>()
        return this as Self
    }
}

@Self
class ReturnTypeWithTypeParameters<T, A, F> {
    fun returnType(): Self {
        return this as Self
    }
}

class InnerClass {
    @Self
    inner class Inner {
        fun returnType(): Self {
            return this as Self
        }
    }
}

class NestedClass {
    @Self
    class Nested {
        fun returnType(): Self {
            return this as Self
        }
    }
}

@Self
class InnerSelfClass {
    inner class Self {
        fun returnSelf(): InnerSelfClass.Self {
            return this
        }
    }

    fun returnType(): Self {
        return this as Self
    }

    fun returnSelfClassType(): InnerSelfClass.Self {
        return InnerSelfClass<InnerSelfClass<*>>().Self()
    }
}

@Self
class TypeAliasSelf {
    @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
    typealias Self = String

    fun returnType(): Self {
        return this as Self
    }

    fun returnTypealias(): TypeAliasSelf.Self {
        return "typealias"
    }
}

@Self
class SelfWithSelfVariable {
    fun returnType(): Self {
        val Self: Self = this as Self
        return Self
    }
}

class InnerClassWithSelfAnnotation<S: InnerClassWithSelfAnnotation<S>> {

    @Self
    inner class SelfAnnotated {
        fun returnType(): Self {
            return this as Self
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun returnType(): S {
        return this as S
    }

}

@Self
interface SelfTypeParameterInterface

@Self
class SelfTypeAsTypeParameterInExtends : SelfTypeParameterInterface<Self> {

    fun returnType(): Self {
        return this as Self
    }
}
