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

fun target(...DerivedProps.$props) {
    id + 1
    label.length
    if (enabled) {}
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, ifExpression, integerLiteral, override,
propertyDeclaration, stringLiteral */
