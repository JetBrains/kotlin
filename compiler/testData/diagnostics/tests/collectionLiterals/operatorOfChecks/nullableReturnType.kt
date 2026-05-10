// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals

class Explicit {
    companion object {
        operator fun of(vararg args: String): <!NULLABLE_RETURN_TYPE_OF_OPERATOR_OF!>Explicit?<!> = null
    }
}

class Implicit {
    companion object {
        fun nullable(): Implicit? = null
        operator fun <!NULLABLE_RETURN_TYPE_OF_OPERATOR_OF!>of<!>(vararg args: String) = nullable()
    }
}

class GenericExplicit<T> {
    companion object {
        operator fun of(vararg args: String): <!NULLABLE_RETURN_TYPE_OF_OPERATOR_OF!>GenericExplicit<String>?<!> = null
    }
}

class GenericImplicit<T> {
    companion object {
        fun nullable(): GenericImplicit<String>? = null
        operator fun <!NULLABLE_RETURN_TYPE_OF_OPERATOR_OF!>of<!>(vararg args: String) = nullable()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration, operator,
typeParameter, vararg */
