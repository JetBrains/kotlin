// LANGUAGE: +CompanionBlocksAndExtensions +CollectionLiterals
// RUN_PIPELINE_TILL: BACKEND

class SingleVararg {
    companion {
        operator fun of(vararg v: Int): SingleVararg = SingleVararg()
    }
}

class VarargAndOthers {
    companion {
        operator fun of(vararg v: Int): VarargAndOthers = VarargAndOthers()
        operator fun of(): VarargAndOthers = VarargAndOthers()
        operator fun of(v: Int): VarargAndOthers = VarargAndOthers()
        operator fun of(a: Int, b: Int): VarargAndOthers = VarargAndOthers()
    }
}

class NonEmptyVararg {
    companion {
        operator fun of(a: Int, vararg v: Int): NonEmptyVararg = NonEmptyVararg()
    }
}

class NonEmptyVarargAndEmpty {
    companion {
        operator fun of(): NonEmptyVarargAndEmpty = NonEmptyVarargAndEmpty()
        operator fun of(a: Int, vararg b: Int): NonEmptyVarargAndEmpty = NonEmptyVarargAndEmpty()
    }
}

class Generic<T> {
    companion {
        operator fun <T> of(): Generic<T> = Generic<T>()
        operator fun <T> of(x: T): Generic<T> = Generic<T>()
        operator fun <T> of(vararg x: T): Generic<T> = Generic<T>()
    }
}

class ImplicitReturnType<T> {
    companion {
        operator fun <T> of(vararg x: T) = ImplicitReturnType<T>()
    }
}

class InternalOf {
    companion {
        internal operator fun of(vararg x: String): InternalOf = InternalOf()
        internal operator fun of(): InternalOf = InternalOf()
    }
}

class InTwoBlocks {
    companion {
        private operator fun of(): InTwoBlocks = InTwoBlocks()
    }

    companion {
        private operator fun of(vararg x: String): InTwoBlocks = InTwoBlocks()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, operator, typeParameter, vararg */
