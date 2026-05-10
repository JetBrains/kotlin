// LANGUAGE: +CompanionBlocksAndExtensions +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND

class NoVararg {
    companion {
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun of(): NoVararg<!> = NoVararg()
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun of(x: Int): NoVararg<!> = NoVararg()
    }
}

class TwoVarargs {
    companion {
        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>operator fun of(vararg x: String): TwoVarargs<!> = TwoVarargs()
        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>operator fun of(vararg x: Char): TwoVarargs<!> = TwoVarargs()
    }
}

class TypeParameterMismatch<out T> {
    companion {
        <!INCONSISTENT_TYPE_PARAMETERS_IN_OF_OVERLOADS!>operator fun of(): TypeParameterMismatch<Nothing> = TypeParameterMismatch()<!>
        operator fun <T> of(vararg t: T): TypeParameterMismatch<T> = TypeParameterMismatch()
    }
}

class ReturnTypeMismatch<out T> {
    companion {
        operator fun <T> of(): <!INCONSISTENT_RETURN_TYPES_IN_OF_OVERLOADS!>ReturnTypeMismatch<Nothing><!> = ReturnTypeMismatch()
        operator fun <T> of(vararg t: T): ReturnTypeMismatch<T> = ReturnTypeMismatch()
    }
}

class VisibilityMismatch {
    companion {
        operator fun <!INCONSISTENT_VISIBILITY_IN_OF_OVERLOADS!>of<!>(): VisibilityMismatch = VisibilityMismatch()
        internal operator fun of(vararg i: Int): VisibilityMismatch = VisibilityMismatch()
    }
}

class NullableReturn {
    companion {
        operator fun of(vararg i: Int): <!NULLABLE_RETURN_TYPE_OF_OPERATOR_OF!>NullableReturn?<!> = null
    }
}

class UnrelatedReturn {
    class Unrelated { }
    companion {
        operator fun of(vararg i: Int): <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF!>Unrelated<!> = Unrelated()
    }
}

class ParameterMismatch {
    companion {
        operator fun of(vararg i: Int): ParameterMismatch = ParameterMismatch()
        operator fun of(<!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS!>i: Long<!>): ParameterMismatch = ParameterMismatch()
    }
}

class SameParameterMismatch {
    companion {
        operator fun of(<!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS!>l: Long<!>, vararg i: Int): SameParameterMismatch = SameParameterMismatch()
    }
}

class BoundMismatch<T> {
    companion {
        operator fun <T> of(vararg i: T): BoundMismatch<T> = BoundMismatch()
        operator fun <!INCONSISTENT_TYPE_PARAMETERS_IN_OF_OVERLOADS!><T: Any><!> of(): BoundMismatch<T> = BoundMismatch()
    }
}

class ReifiednessMismatch {
    companion {
        operator fun <T> of(vararg i: T): ReifiednessMismatch = ReifiednessMismatch()
        inline operator fun <!INCONSISTENT_TYPE_PARAMETERS_IN_OF_OVERLOADS!><reified T><!> of(): ReifiednessMismatch = ReifiednessMismatch()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline, nestedClass, nullableType, operator, out,
primaryConstructor, reified, typeConstraint, typeParameter, vararg */
