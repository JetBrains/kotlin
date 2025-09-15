// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80936

// FILE: internal.kt
package internal

@PublishedApi
internal fun interface Foo {
    suspend fun close()
}

internal typealias TA = Foo

// FILE: use.kt
import internal.*

suspend inline fun use() {
    val foo = Foo {}
    val ta = TA {}
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, inline, interfaceDeclaration, lambdaLiteral, localProperty,
propertyDeclaration, suspend */
