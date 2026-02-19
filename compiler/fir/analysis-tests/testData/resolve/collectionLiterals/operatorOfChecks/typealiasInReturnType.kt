// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals, +NestedTypeAliases

object Obj
typealias Irrelevant = Obj

class Explicit {
    companion object {
        operator fun of(vararg args: String): <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF!>Irrelevant<!> = Irrelevant
    }
}

class Implicit {
    companion object {
        operator fun <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF!>of<!>(vararg args: String) = Irrelevant
    }
}

class GenericExplicit<T> {
    companion object {
        operator fun of(vararg args: String): <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF!>Irrelevant<!> = Irrelevant
    }
}

class GenericImplicit<T> {
    companion object {
        operator fun <!RETURN_TYPE_MISMATCH_OF_OPERATOR_OF!>of<!>(vararg args: String) = Irrelevant
    }
}

class Correct {
    typealias Relevant = Correct
    companion object {
        operator fun of(vararg args: String): Relevant = Relevant()
        operator fun of() = Relevant()
    }
}

class GenericCorrect<T> {
    typealias GenericRelevant<U> = GenericCorrect<U>
    typealias Relevant = GenericCorrect<String>

    companion object {
        operator fun of(): Relevant = Relevant()
        operator fun of(a1: String) = Relevant()
        operator fun of(a1: String, a2: String): GenericRelevant<String> = GenericRelevant()
        operator fun of(vararg args: String) = GenericRelevant<String>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration, operator,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter, vararg */
