// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +CollectionLiterals

class Irrelevant

class Explicit {
    companion object {
        operator fun of(vararg args: String): <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF("Explicit")!>Irrelevant<!> = Irrelevant()
    }
}

class Implicit {
    companion object {
        operator fun <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF("Implicit")!>of<!>(vararg args: String) = Irrelevant()
    }
}

class GenericExplicit<T> {
    companion object {
        operator fun of(vararg args: String): <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF("GenericExplicit")!>Irrelevant<!> = Irrelevant()
    }
}

class GenericImplicit<T> {
    companion object {
        operator fun <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF("GenericImplicit")!>of<!>(vararg args: String) = Irrelevant()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, operator, typeParameter, vararg */