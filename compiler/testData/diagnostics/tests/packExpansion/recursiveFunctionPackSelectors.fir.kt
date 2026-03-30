// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

open class Props {
    val a: Int = 0
    val b: String = ""
}

fun leaf(...Props.$props) {}

fun mid(...leaf.$props) {
    a + 1
    b.length
}

fun top(...mid.$props) {
    a + 1
    b.length
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration,
stringLiteral */
