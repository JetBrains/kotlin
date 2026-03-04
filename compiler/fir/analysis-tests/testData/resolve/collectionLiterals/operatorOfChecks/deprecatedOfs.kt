// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

class VarargOverloadDeprecated {
    companion object {
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun of()<!> = VarargOverloadDeprecated()
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun of(x: Int)<!> = VarargOverloadDeprecated()

        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(vararg x: Int) = VarargOverloadDeprecated()
    }
}

class MultipleVarargOverloadsDeprecated {
    companion object {
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun of()<!> = MultipleVarargOverloadsDeprecated()

        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(vararg x: Int) = MultipleVarargOverloadsDeprecated()

        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(vararg x: Long) = MultipleVarargOverloadsDeprecated()

        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(vararg x: Char) = MultipleVarargOverloadsDeprecated()
    }
}

class UnrelatedOverloadDeprecated {
    companion object {
        operator fun of() = UnrelatedOverloadDeprecated()

        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(x: Int) = UnrelatedOverloadDeprecated()

        operator fun of(vararg x: Long) = UnrelatedOverloadDeprecated()
    }
}

class DeprecatedWithWrongReturnType {
    companion object {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(): <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF!>DeprecatedWithWrongReturnType.Companion<!> = this
    }
}

class DeprecatedWithDifferentParameterTypes {
    companion object {
        // generally, what strategy to apply here, since there is no main "vararg" parameter
        // probably, the best approximation would be to check parameters between themselves
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(x: Int, y: Long) = DeprecatedWithDifferentParameterTypes()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, operator,
stringLiteral, thisExpression, vararg */
