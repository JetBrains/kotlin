// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CollectionLiterals
// DIAGNOSTICS: -UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR
//  failure reason: KT-81777, KT-76150

fun <T> takeGenericArray(arr: Array<T>): T {
    return arr[0]
}

fun <T> runGenericArray(arr: () -> Array<T>): T {
    return arr()[0]
}

class Box<T> {
    fun put(t: T) {
    }

    fun putAll(arr: Array<T>) {
    }
}

fun <T> buildBox(block: Box<T>.() -> Unit): Box<T> {
    return Box<T>().apply { block() }
}


fun test() {
    takeGenericArray([])
    takeGenericArray(["42"])
    takeGenericArray([42])
    takeGenericArray([{}])
    takeGenericArray([::test])
    takeGenericArray<String>([])
    val resTake: String = takeGenericArray([])
    val errorTake: String = takeGenericArray([42])
    val errorLamTake: String = takeGenericArray([{}])

    runGenericArray { [] }
    runGenericArray { ["42"] }
    runGenericArray { [42] }
    runGenericArray { [{}] }
    runGenericArray { [::test] }
    runGenericArray<String> { [] }
    val resRun: String = runGenericArray { [] }
    val errorRun: String = runGenericArray { [42] }
    val errorLamRun: String = runGenericArray { [{}] }

    buildBox { put([]) }
    buildBox { put(["42"]) }
    buildBox { put([42]) }
    buildBox { put([{}]) }
    buildBox { put([::test]) }

    buildBox { putAll([]) }
    buildBox { putAll(["42"]) }
    buildBox { putAll([42]) }
    buildBox { putAll([{}]) }
    buildBox { putAll([::test]) }
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, nullableType, stringLiteral, typeParameter, typeWithExtension */
