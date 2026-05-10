// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidOperatorEqualsInEnumEntriesAndAnonymousObjects

fun test() {
    val a = object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun String.equals(): Boolean = true
    }
    val b = object {
        suspend <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals() { }
    }
    val c = object {
        override operator fun equals(other: Any?): Boolean = true
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, funWithExtensionReceiver, functionDeclaration, localProperty, operator,
propertyDeclaration, suspend */
