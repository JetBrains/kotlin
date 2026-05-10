// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTIC_ARGUMENTS

class ErrorRegular {
    companion object {
        operator fun of(<!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("Int")!>p1: <!UNRESOLVED_REFERENCE("Unresolved")!>Unresolved<!><!>, vararg ps: Int): ErrorRegular = ErrorRegular()
    }
}

class ErrorVararg {
    companion object {
        operator fun of(<!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("??? (Unresolved qualified name: Unresolved)")!>p1: Int<!>, vararg ps: <!UNRESOLVED_REFERENCE("Unresolved")!>Unresolved<!>): ErrorVararg = ErrorVararg()
    }
}

class ErrorBoth {
    companion object {
        operator fun of(<!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("??? (Unresolved qualified name: Unresolved)")!>p1: <!UNRESOLVED_REFERENCE("Unresolved1")!>Unresolved1<!><!>, vararg ps: <!UNRESOLVED_REFERENCE("Unresolved")!>Unresolved<!>): ErrorBoth = ErrorBoth()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, operator, vararg */
