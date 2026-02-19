// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-51538
// WITH_STDLIB

suspend fun other() {}

// Case 1: Non-suspend local function calling suspend functions through flatMap (inline function)
// The original issue reported this case compiles but shouldn't
suspend fun outer1(a: List<Int>, acc: List<Int>): List<Int> {
    fun inner(): List<Int> {
        return a.flatMap { other(); outer1(a, acc + listOf(it)) }
    }

    return inner()
}

// Case 2: Same with a custom inline function - the original issue reported this correctly fails
inline fun <T> Iterable<T>.fmap(block: (T) -> List<T>) = flatMap(block)

suspend fun outer2(a: List<Int>, acc: List<Int>): List<Int> {
    fun inner(): List<Int> {
        return a.fmap { <!NON_LOCAL_SUSPENSION_POINT!>other<!>(); <!NON_LOCAL_SUSPENSION_POINT!>outer2<!>(a, acc + listOf(it)) }
    }

    return inner()
}

// Case 3: Using map instead of flatMap - the original issue reported this correctly fails
suspend fun outer3(a: List<Int>, acc: List<Int>): List<Int> {
    fun inner(): List<List<Int>> {
        return a.map { <!NON_LOCAL_SUSPENSION_POINT!>other<!>(); <!NON_LOCAL_SUSPENSION_POINT!>outer3<!>(a, acc + listOf(it)) }
    }

    return inner().flatten()
}

/* GENERATED_FIR_TAGS: additiveExpression, funWithExtensionReceiver, functionDeclaration, functionalType, inline,
lambdaLiteral, localFunction, nullableType, suspend, typeParameter */
