// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

typealias IterableClass<C, T> = (C) -> Iterator<T>

context(IterableClass<C, T>)
fun <C, T> C.iterator(any: Any?): Iterator<T> = this<!UNRESOLVED_LABEL!>@IterableClass<!>.invoke(this)

fun <T> listOf(vararg items: T): List<T> = null!!

fun test() {
    val f: IterableClass<List<Int>, Int> = {
        it.listIterator()
    }
    with(f) {
        listOf(1, 2, 3).iterator(null)
    }
    listOf(1, 2, 3).<!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>iterator<!>(null)
}

/* GENERATED_FIR_TAGS: checkNotNullCall, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration, thisExpression,
typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter, vararg */
