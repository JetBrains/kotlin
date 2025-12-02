// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82869
fun interface MySupplier<T> {
    fun get(): T
}

fun foo(supplier: suspend () -> String): String = ""
fun foo(supplier: MySupplier<String>): Any = Any()

suspend fun bar(): String = ""

fun main() {
    foo {
        <!ILLEGAL_SUSPEND_FUNCTION_CALL!>bar<!>() // Shouldn't be ILLEGAL_SUSPEND_FUNCTION_CALL?
    }.<!UNRESOLVED_REFERENCE!>length<!>
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
nullableType, samConversion, stringLiteral, suspend, typeParameter */
