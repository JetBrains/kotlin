// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
//KT-2320 failure of complex case of type inference
package i

interface NotMap<B>

interface Entry<B> {
    fun getValue(): B
}


fun <V, R> NotMap<V>.mapValuesOriginal(ff: (Entry<V>) -> R): NotMap<R> = throw Exception()

fun <B, C> NotMap<B>.mapValuesOnly(f: (B) -> C) = mapValuesOriginal { e -> f(e.getValue()) }

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, interfaceDeclaration,
lambdaLiteral, nullableType, typeParameter */
