// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidOperatorEqualsInEnumEntriesAndAnonymousObjects
// RENDER_DIAGNOSTICS_FULL_TEXT

fun test() {
    val a = object {
        <!INAPPLICABLE_OPERATOR_MODIFIER_WARNING!>operator<!> fun String.equals(): Boolean = true
    }
    val b = object {
        suspend <!INAPPLICABLE_OPERATOR_MODIFIER_WARNING!>operator<!> fun equals() { }
    }
    val c = object {
        override operator fun equals(other: Any?): Boolean = true
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, funWithExtensionReceiver, functionDeclaration, localProperty, operator,
propertyDeclaration, suspend */
