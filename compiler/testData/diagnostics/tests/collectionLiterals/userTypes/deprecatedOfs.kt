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
        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of(vararg x: Int) = OldSetDeprecated()

        @Deprecated("", level = DeprecationLevel.HIDDEN)
        operator fun of() = OldSetDeprecated()

        operator fun of(vararg x: Long) = OldSetDeprecated()
        operator fun of(x: Long) = OldSetDeprecated()
    }
}

fun <T> take(t: T) = Unit

fun test() {
    take<AllDeprecated>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3]<!>)
    take<AllDeprecated>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)

    take<OldSetDeprecated>([1, 2, 3])
    take<OldSetDeprecated>([])

    val x = when {
        true -> AllDeprecated()
        else -> [1, 2, 3]
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
