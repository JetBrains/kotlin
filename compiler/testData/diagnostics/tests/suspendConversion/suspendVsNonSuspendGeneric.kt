// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82869
fun interface MySupplier<T> {
    fun get(): T
}

fun foo(supplier: suspend () -> String): String = ""
fun <T> foo(supplier: () -> String): T = TODO()

suspend fun bar(): String = ""

fun main() {
    val x: String = foo {
        bar() // Shouldn't be ILLEGAL_SUSPEND_FUNCTION_CALL?
    }

    x.length
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, stringLiteral, suspend, typeParameter */
