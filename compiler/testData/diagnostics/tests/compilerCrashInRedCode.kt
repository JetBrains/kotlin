// RUN_PIPELINE_TILL: FRONTEND
fun foo(x: Int.() -> Unit) { }

fun test(){
    foo(Int.<!ILLEGAL_SELECTOR!>{}<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, typeWithExtension */
