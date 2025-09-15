// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80936

// FILE: internal.kt
package internal

@PublishedApi
internal fun interface Foo {
    suspend fun close()
}

// FILE: use.kt
import internal.Foo

suspend inline fun use() {
    val foo = Foo {}
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, inline, interfaceDeclaration, lambdaLiteral, localProperty,
propertyDeclaration, suspend */
