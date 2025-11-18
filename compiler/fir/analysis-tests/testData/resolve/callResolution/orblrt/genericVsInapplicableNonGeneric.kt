// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-61580

class Type1<B>
class Type2

@JvmName("funA2")
fun <B> String.funA(selector: Any?.(Any?) -> Type1<B>) {}

@JvmName("funA3")
fun String.funA(selector: Any?.(Any?) -> Type2) {}

fun main() {
    "".funA { Type1<Int>() } // Type mismatch, expects Type2
    "".funA { Type2() } // works
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral,
nullableType, stringLiteral, typeParameter, typeWithExtension */
