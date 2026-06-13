// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-80592

fun test() {
    val x = object {
        @Deprecated("", level = DeprecationLevel.ERROR)
        val y: String
            get() = ""
    }
    x.<!DEPRECATION_ERROR!>y<!>
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, localProperty, propertyDeclaration, stringLiteral */
