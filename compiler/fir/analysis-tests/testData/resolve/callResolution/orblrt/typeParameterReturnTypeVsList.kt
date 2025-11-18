// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-23883

@JvmName("foo1")
fun <T> foo/* aka foo1 */(action: () -> T): T = action()
fun <T> foo/* aka foo2 */(action: () -> List<T>): T = action().first()

fun test(){
    foo{ 1 } // wants to call foo2 instead of foo1
    foo{ listOf(1) }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, nullableType, stringLiteral,
typeParameter */
