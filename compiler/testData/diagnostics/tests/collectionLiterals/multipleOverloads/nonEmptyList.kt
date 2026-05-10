// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

class NonEmptyList {
    companion object {
        operator fun of(x: Int, vararg y: Int) = NonEmptyList()
        operator fun of(x: Int) = NonEmptyList()
    }
}

class CanBeEmptyList {
    companion object {
        operator fun of(vararg y: Int) = CanBeEmptyList()
        operator fun of(y: Int) = CanBeEmptyList()
    }
}

fun f0(x: NonEmptyList) { }

fun f1(x: CanBeEmptyList) { }
fun f1(x: NonEmptyList) { }

fun test() {
    f0(<!NO_VALUE_FOR_PARAMETER!>[]<!>)

    f1([])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f1<!>(<!UNRESOLVED_REFERENCE!>[42]<!>)
    <!NONE_APPLICABLE!>f1<!>(<!UNRESOLVED_REFERENCE!>[42.0]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, objectDeclaration,
operator, vararg */
