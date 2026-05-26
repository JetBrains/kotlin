// RUN_PIPELINE_TILL: BACKEND
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
    handle(x)
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral, propertyDeclaration,
samConversion, stringLiteral */
