// RUN_PIPELINE_TILL: FRONTEND
fun interface Run {
    fun run()
}

fun handle(run: Run) {
    //...
}

val x = {
    "STRING"
}

fun test() {
    handle(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral, propertyDeclaration,
samConversion, stringLiteral */
