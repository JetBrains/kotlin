// FIR_DISABLE_LAZY_RESOLVE_CHECKS
import kotlin.Self

class InnerClass {
    @Self
    inner class Inner {
        fun returnType(): Self {
            return this
        }
    }
}

class NestedClass {
    @Self
    class Nested {
        fun returnType(): Self {
            return this
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
        return this
    }

    fun returnSelfClassType(): InnerSelfClass.Self {
        return InnerSelfClass<InnerSelfClass<*>>().Self()
    }
}


class InnerClassWithSelfAnnotation<S: InnerClassWithSelfAnnotation<S>> {

    @Self
    inner class SelfAnnotated {
        fun returnType(): Self {
            return this
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun returnType(): S {
        return this as S
    }

}

@Self
class QualifiedThisClass {
    inner class Inner {
        fun foo(): Self {
            return this@QualifiedThisClass
        }
    }
}