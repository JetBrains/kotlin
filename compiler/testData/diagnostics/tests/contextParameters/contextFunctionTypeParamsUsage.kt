// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-88717
fun <T> f(t: @ContextFunctionTypeParams(42) T, tt: @ContextFunctionTypeParams(1) Int) {}

fun test() {
    val f: @ContextFunctionTypeParams(1) @ExtensionFunctionType (Int, String) -> Unit = {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, typeParameter */
