// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-66229

fun foo() {
    buildMap {
        for (v in this) {
            put(1, 1)
        }
    }
}

fun bar() {
    buildMap {
        mapValues { (key: Int, value: String) -> "1" }
    }
}

/* GENERATED_FIR_TAGS: forLoop, functionDeclaration, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, thisExpression */
