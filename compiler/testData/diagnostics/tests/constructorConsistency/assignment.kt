// RUN_PIPELINE_TILL: BACKEND
class My {
    val x: String

    constructor() {
        val temp = <!DEBUG_INFO_LEAKING_THIS!>this<!>
        x = bar(temp)
    }

}

fun bar(arg: My) = arg.x

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, localProperty, propertyDeclaration,
secondaryConstructor, thisExpression */
