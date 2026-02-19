// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-78595

open class ReturnSuperType
open class ReturnSubType: ReturnSuperType()

open class SuperClass {
    open val property: ReturnSuperType = ReturnSuperType()
}

open class SubClass: SuperClass() {
    override val property: ReturnSubType = ReturnSubType()
}

var variable: SuperClass = SubClass()

fun test() {
    if (variable is SubClass) {
        // smartcast to 'SubClass' is unstable,
        // but `property` is also available from `SuperClass`,
        // so there should not be any error
        <!DEBUG_INFO_EXPRESSION_TYPE("ReturnSuperType")!>variable.property<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, override, propertyDeclaration, smartcast */
