// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

class VarargOverloadDeprecated {
    companion object {
        operator fun of() = VarargOverloadDeprecated()
        operator fun of(x: Int) = VarargOverloadDeprecated()

        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(vararg x: Int) = VarargOverloadDeprecated()
    }
}

class MultipleVarargOverloadsDeprecated {
    companion object {
        operator fun of() = MultipleVarargOverloadsDeprecated()

        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>@Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(vararg x: Int)<!> = MultipleVarargOverloadsDeprecated()

        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>@Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(vararg x: Long)<!> = MultipleVarargOverloadsDeprecated()

        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>@Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(vararg x: Char)<!> = MultipleVarargOverloadsDeprecated()
    }
}

class UnrelatedOverloadDeprecated {
    companion object {
        operator fun of() = UnrelatedOverloadDeprecated()

        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(<!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS!>x: Int<!>) = UnrelatedOverloadDeprecated()

        operator fun of(vararg x: Long) = UnrelatedOverloadDeprecated()
    }
}

class DeprecatedWithWrongReturnType {
    companion object {
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>@Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(): <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF!>DeprecatedWithWrongReturnType.Companion<!><!> = this
    }
}

class DeprecatedWithDifferentParameterTypes {
    companion object {
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>@Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(x: Int, y: Long)<!> = DeprecatedWithDifferentParameterTypes()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, operator,
stringLiteral, thisExpression, vararg */
