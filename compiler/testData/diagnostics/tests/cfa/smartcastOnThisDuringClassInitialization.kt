// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-56863
interface I

open class Some {
    val x: Int

    init {
        this as I
        x = 1
    }
}

/* GENERATED_FIR_TAGS: asExpression, assignment, classDeclaration, init, integerLiteral, interfaceDeclaration,
propertyDeclaration, thisExpression */
