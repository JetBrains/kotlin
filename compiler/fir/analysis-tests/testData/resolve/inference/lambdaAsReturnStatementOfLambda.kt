// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-37070

class A

fun test(a: A) {

    val lambda = a.let {
        { it }
    }

    val alsoA = lambda()
    takeA(alsoA)
}

fun takeA(a: A) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, lambdaLiteral, localProperty, propertyDeclaration */
