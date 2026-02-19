// FIR_DUMP
// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

fun main() {
    val c: context(Int, String) () -> Unit = {}
    val d: Int.(String) -> Unit = {}

    val x = merge(c, d)
    val y = merge(d, c)

    consumeContextualFunctionList(x)
    consumeContextualFunctionList(y)

    consumeExtensionFunctionList(x)
    consumeExtensionFunctionList(y)
}

class MyList<T>
fun <T> merge(vararg values: T): MyList<T> = MyList()

fun consumeContextualFunctionList(obj: MyList<context(Int, String) () -> Unit>) {}
fun consumeExtensionFunctionList(obj: MyList<Int.(String) -> Unit>) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, typeParameter, typeWithContext, typeWithExtension, vararg */
