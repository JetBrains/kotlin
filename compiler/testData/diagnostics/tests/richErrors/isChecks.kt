// RUN_PIPELINE_TILL: FRONTEND
error class Foo

fun test(x: String | Foo, y: String | RichError) {
    if (x is String) return
    if (x is Foo) return
    if (x|.length !is Foo) return
    if (y !is Foo) return
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, isExpression, smartcast */
