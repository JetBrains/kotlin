// RUN_PIPELINE_TILL: FRONTEND
class My {
    var x: String = ""
        set(<!WRONG_MODIFIER_CONTAINING_DECLARATION!>vararg<!> value) {
            x = value
        }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, outProjection, propertyDeclaration, setter, stringLiteral, vararg */
