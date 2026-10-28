// RUN_PIPELINE_TILL: CODEGEN
// FIR_DUMP
error class Foo

typealias R = String | Foo

fun test1(x: R?) {
    if (x == null) return
    x|.length
}

fun test2(x: R?, b: Boolean) {
    if (b) {
        if (x == null) return
    } else {
        if (x == null) return
    }
    x|.length
}

fun test3(x: R?) {
    if (x != null) {
        x|.length
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, nullableType, safeCall,
smartcast, typeAliasDeclaration */
