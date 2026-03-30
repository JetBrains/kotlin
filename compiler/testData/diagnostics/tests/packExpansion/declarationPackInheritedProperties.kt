// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

open class BaseProps {
    open val id: Int = 0
    val label: String = ""
}

class DerivedProps : BaseProps() {
    override val id: Int = 1
    val enabled: Boolean = false
}

fun target(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>DerivedProps<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, ifExpression, integerLiteral, override,
propertyDeclaration, stringLiteral */
