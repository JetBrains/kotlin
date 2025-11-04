// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80673

fun owner() {
    class Local(param: Any) {
        val x: Any <!DELEGATE_SPECIAL_FUNCTION_MISSING!>by<!> param
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, primaryConstructor, propertyDeclaration,
propertyDelegate, starProjection */
