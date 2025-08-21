// RUN_PIPELINE_TILL: FRONTEND
data class A(val x: Set<<!UNRESOLVED_REFERENCE!>CLassNotFound<!>> = setOf()) {
    fun with(x: Set<<!UNRESOLVED_REFERENCE!>CLassNotFound<!>>? = null) {
        A(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> ?: this.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, elvisExpression, functionDeclaration, nullableType, primaryConstructor,
propertyDeclaration, thisExpression */
