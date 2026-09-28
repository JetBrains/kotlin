// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// ISSUE: KT-54668

interface A {
    val list: List<String>
}

fun getA(): A? = null

val x by lazy {
    (getA() ?: error("error")).list.associateBy {
        it
    }
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, interfaceDeclaration, lambdaLiteral, nullableType,
propertyDeclaration, propertyDelegate, stringLiteral */
