// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

fun <T> T.test(block: (foo: T) -> Unit) {}
fun <K> K.rest(block: (bar: K) -> Unit) {}

fun main() {
    10.test { a ->
        a.rest { b ->
        }
    }
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
nullableType, typeParameter */
