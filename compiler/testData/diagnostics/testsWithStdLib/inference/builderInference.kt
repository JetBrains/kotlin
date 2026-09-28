// RUN_PIPELINE_TILL: CODEGEN
fun <T> foo(block: MutableList<T>.() -> Unit): T = null!!

fun takeString(s: String) {}

fun test() {
    val s = foo {
        this.add("")
    }
    takeString(s)
}

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, functionalType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, thisExpression, typeParameter, typeWithExtension */
