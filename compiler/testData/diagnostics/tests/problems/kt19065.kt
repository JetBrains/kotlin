// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-19065
// WITH_STDLIB

// KT-19065: Do not report "destructured parameter is never used" on empty lambda
fun foo(map: Map<String, Int>) {
    map.forEach { (key, value) ->

    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, localProperty, propertyDeclaration */
