// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTIC_ARGUMENTS

class ErrorInMain {
    companion object {
        operator fun of(vararg vs: Int): <!UNRESOLVED_REFERENCE("Unresolved")!>Unresolved<!> = null
        operator fun of(): <!INCONSISTENT_RETURN_TYPES_IN_OF_OVERLOADS("??? (Unresolved qualified name: Unresolved)")!>ErrorInMain<!> = ErrorInMain()
    }
}

class ErrorInMainTypeArgument<T> {
    companion object {
        operator fun of(vararg vs: Int): ErrorInMainTypeArgument<<!UNRESOLVED_REFERENCE("Unresolved")!>Unresolved<!>> = <!CANNOT_INFER_PARAMETER_TYPE("T")!>ErrorInMainTypeArgument<!>()
        operator fun of(): <!INCONSISTENT_RETURN_TYPES_IN_OF_OVERLOADS("ErrorInMainTypeArgument<??? (Unresolved qualified name: Unresolved)>")!>ErrorInMainTypeArgument<Nothing><!> = ErrorInMainTypeArgument()
    }
}

class ErrorInOther {
    companion object {
        operator fun of(vararg vs: Int): ErrorInOther = ErrorInOther()
        operator fun of(): <!INCONSISTENT_RETURN_TYPES_IN_OF_OVERLOADS("ErrorInOther"), UNRESOLVED_REFERENCE("Unresolved")!>Unresolved<!> = null
    }
}

class ErrorInOtherTypeArgument<T> {
    companion object {
        operator fun of(vararg vs: Int): ErrorInOtherTypeArgument<Nothing> = ErrorInOtherTypeArgument()
        operator fun of(): <!INCONSISTENT_RETURN_TYPES_IN_OF_OVERLOADS("ErrorInOtherTypeArgument<Nothing>")!>ErrorInOtherTypeArgument<<!UNRESOLVED_REFERENCE("Unresolved")!>Unresolved<!>><!> = <!CANNOT_INFER_PARAMETER_TYPE("T")!>ErrorInOtherTypeArgument<!>()
    }
}

class ErrorInBoth {
    companion object {
        operator fun of(vararg vs: Int): <!UNRESOLVED_REFERENCE("Unresolved")!>Unresolved<!> = null
        operator fun of(): <!INCONSISTENT_RETURN_TYPES_IN_OF_OVERLOADS("??? (Unresolved qualified name: Unresolved)"), UNRESOLVED_REFERENCE("Unresolved2")!>Unresolved2<!> = null
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration, operator,
typeParameter, vararg */
