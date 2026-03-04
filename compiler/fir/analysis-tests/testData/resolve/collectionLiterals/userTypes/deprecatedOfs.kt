// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

class AllDeprecated {
    companion object {
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(vararg x: Int) = AllDeprecated()

        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of() = AllDeprecated()
    }
}

class OldSetDeprecated {
    companion object {
        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>@Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(vararg x: Int)<!> = OldSetDeprecated()

        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of() = OldSetDeprecated()

        <!MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF!>operator fun of(vararg x: Long)<!> = OldSetDeprecated()
        operator fun of(x: Long) = OldSetDeprecated()
    }
}

fun <T> take(t: T) = Unit

fun test() {
    take<AllDeprecated>(<!NONE_APPLICABLE, UNRESOLVED_REFERENCE!>[1, 2, 3]<!>)
    take<AllDeprecated>(<!UNRESOLVED_REFERENCE!>[]<!>)

    take<OldSetDeprecated>([1, 2, 3])
    take<OldSetDeprecated>([])

    val x = when {
        true -> AllDeprecated()
        else -> <!NONE_APPLICABLE, UNRESOLVED_REFERENCE!>[1, 2, 3]<!>
    }

    val y = when {
        true -> OldSetDeprecated()
        else -> [1, 2, 3]
    }

    val z = when {
        true -> OldSetDeprecated()
        else -> [42]
    }

    val t = when {
        true -> OldSetDeprecated()
        else -> []
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, localProperty,
nullableType, objectDeclaration, operator, propertyDeclaration, stringLiteral, typeParameter, vararg, whenExpression */
